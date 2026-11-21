package dev.reminishub.config;

import dev.reminishub.model.entity.Usuario;
import dev.reminishub.repository.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.Locale;

@Configuration
public class SecurityConfig {
    /** Rutas que cualquier visitante puede leer; se listan una vez para GET y HEAD. */
    private static final String[] PUBLICAS = {
            "/", "/proyectos", "/proyectos/*", "/catalogo/resultados",
            "/css/**", "/js/**", "/vendor/**",
            "/api/v1/ping", "/api/v1/proyectos", "/api/v1/proyectos/*",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    /**
     * Cuentas locales del registro. El hash nunca sale del servidor y una cuenta sin
     * contrasena local (por ejemplo, la semilla de demostracion) no puede iniciar sesion.
     */
    @Bean
    public UserDetailsService cuentasLocales(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email == null ? "" : email.strip().toLowerCase(Locale.ROOT))
                .map(SecurityConfig::aUsuarioDeSeguridad)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales no validas."));
    }

    private static org.springframework.security.core.userdetails.UserDetails aUsuarioDeSeguridad(Usuario usuario) {
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("La cuenta no admite login local.");
        }
        return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .roles(usuario.getRol().name())
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }

    /** Un unico proveedor: credenciales locales comparadas con BCrypt. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService cuentas, PasswordEncoder codificador) {
        var proveedor = new DaoAuthenticationProvider(cuentas);
        proveedor.setPasswordEncoder(codificador);
        return new ProviderManager(proveedor);
    }

    /**
     * Catalogo y registro publicos; login y logout propios; el resto exige JWT.
     * La autenticacion viaja en cookie, por eso CSRF sigue activo y no hay sesion de seguridad.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Converter<Jwt, AbstractAuthenticationToken> jwtConverter,
                                                   org.springframework.security.oauth2.jwt.JwtDecoder decodificador,
                                                   UsuarioRepository usuarios, dev.reminishub.repository.SesionesRevocadas revocadas)
            throws Exception {
        return http.redirectToHttps(https -> https.requestMatchers(
                        PathPatternRequestMatcher.withDefaults().matcher("/registro"),
                        PathPatternRequestMatcher.withDefaults().matcher("/login"),
                        PathPatternRequestMatcher.withDefaults().matcher("/propuestas/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/mis-propuestas"),
                        PathPatternRequestMatcher.withDefaults().matcher("/admin/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/v1/propuestas/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/api/v1/admin/**"),
                        PathPatternRequestMatcher.withDefaults().matcher("/logout")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/registro", "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/registro", "/login").permitAll()
                        .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/mis-propuestas", "/propuestas/**", "/api/v1/propuestas/**").authenticated()
                        .requestMatchers(HttpMethod.GET, PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.HEAD, PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/sesion").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/csrf").authenticated()
                        .requestMatchers(HttpMethod.POST, "/logout").authenticated()
                        .anyRequest().denyAll())
                // El logout propio borra la cookie del JWT; el filtro estandar no la conoce.
                .logout(fin -> fin.disable())
                // Todo metodo inseguro exige token CSRF, incluso con sesion iniciada.
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(peticion ->
                        !java.util.Set.of("GET", "HEAD", "TRACE", "OPTIONS")
                                .contains(peticion.getMethod().toUpperCase(java.util.Locale.ROOT))))
                // El filtro JWT autentica cada petición; no debe disparar un nuevo login de sesión.
                .sessionManagement(sesion -> sesion.requireExplicitAuthenticationStrategy(true))
                .securityContext(context -> context.securityContextRepository(
                        new org.springframework.security.web.context.NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(
                        new org.springframework.security.web.savedrequest.NullRequestCache()))
                // Autenticacion propia desde la cookie, DESPUES de CsrfFilter: un POST sin token
                // se rechaza en 403 antes de que el JWT llegue a conceder identidad.
                .addFilterAfter(new JwtCookieAuthenticationFilter(decodificador, jwtConverter, usuarios, revocadas),
                        org.springframework.security.web.csrf.CsrfFilter.class)
                .exceptionHandling(errores -> errores
                        // El contrato REST depende de la ruta, no del Accept enviado por el cliente.
                        .defaultAuthenticationEntryPointFor((peticion, respuesta, excepcion) -> respuesta.sendError(401),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                        // En el navegador se ofrece el formulario; la API conserva 401 sin redireccion.
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML))
                        .defaultAuthenticationEntryPointFor((peticion, respuesta, excepcion) -> respuesta.sendError(401),
                                AnyRequestMatcher.INSTANCE))
                .build();
    }
}
