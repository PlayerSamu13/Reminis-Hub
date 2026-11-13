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

@Configuration
public class WebConfig implements WebMvcConfigurer {
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
                if ("GET".equals(request.getMethod()) && ("es".equals(lang) || "en".equals(lang)))
                    localeResolver().setLocale(request, response, Locale.forLanguageTag(lang));
                return true;
            }
        });
    }
}
