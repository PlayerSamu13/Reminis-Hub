package dev.reminishub.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    /** Solo lectura pública explícita. CSRF permanece activo; autenticación propia viene en E2. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/proyectos", "/proyectos/*", "/catalogo/resultados",
                        "/css/**", "/js/**", "/vendor/**", "/api/v1/ping",
                        "/api/v1/proyectos", "/api/v1/proyectos/*").permitAll()
                .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(401)))
                .build();
    }
}
