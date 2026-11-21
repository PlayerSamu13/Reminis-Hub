package dev.reminishub.config;

import dev.reminishub.service.AccesoGoogle;
import dev.reminishub.service.TokenService;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

/** Spring gestiona el intercambio OIDC. La aplicación emite su cookie tras resolver la cuenta. */
@Configuration
@Profile("google")
public class GoogleConfig {
    @Bean
    @Order(1)
    SecurityFilterChain cadenaSeguridadGoogle(HttpSecurity http, AccesoGoogle acceso, TokenService tokens,
                                             ClientRegistrationRepository clientes) throws Exception {
        var solicitudes = new DefaultOAuth2AuthorizationRequestResolver(clientes, "/oauth2/authorization");
        solicitudes.setAuthorizationRequestCustomizer(solicitud -> solicitud.additionalParameters(
                parametros -> parametros.put("prompt", "select_account")));
        return http.securityMatcher("/oauth2/authorization/google", "/login/oauth2/code/google")
                .redirectToHttps(https -> {})
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll())
                .oauth2Login(login -> login.loginPage("/login")
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(solicitudes))
                        .failureUrl("/login?googleError")
                        .successHandler((peticion, respuesta, autenticacion) -> {
                            try {
                                var identidad = (OidcUser) autenticacion.getPrincipal();
                                String email = acceso.cuentaLocal(identidad);
                                respuesta.addHeader(HttpHeaders.SET_COOKIE, tokens.cookie(tokens.emitir(email)).toString());
                                respuesta.sendRedirect(peticion.getContextPath() + "/proyectos");
                            } catch (AuthenticationException | org.springframework.dao.DataAccessException
                                     | org.springframework.security.oauth2.jwt.JwtEncodingException conflicto) {
                                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                                respuesta.sendRedirect(peticion.getContextPath() + "/login?googleError");
                            }
                        }))
                .build();
    }
}
