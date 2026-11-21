package dev.reminishub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import java.util.Locale;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    /** Idiomas con bundle propio; cualquier otro valor de ?lang se ignora. */
    private static final Set<String> IDIOMAS = Set.of("es", "en", "eo");

    @Bean
    public CookieLocaleResolver localeResolver() {
        var resolver = new CookieLocaleResolver("reminishub_lang");
        resolver.setDefaultLocale(Locale.forLanguageTag("es"));
        resolver.setCookieHttpOnly(true);
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String lang = request.getParameter("lang");
                if ("GET".equals(request.getMethod()) && lang != null && IDIOMAS.contains(lang))
                    localeResolver().setLocale(request, response, Locale.forLanguageTag(lang));
                return true;
            }
        });
    }
}
