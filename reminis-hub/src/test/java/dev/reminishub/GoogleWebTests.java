package dev.reminishub;

import dev.reminishub.config.GoogleConfig;
import dev.reminishub.controller.api.ControllerREST;
import dev.reminishub.repository.IdentidadExternaRepository;
import dev.reminishub.repository.UsuarioRepository;
import dev.reminishub.service.AccesoGoogle;
import dev.reminishub.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ControllerREST.class)
@ActiveProfiles("google")
@Import({GoogleConfig.class, GoogleWebTests.Cliente.class})
class GoogleWebTests {
    @TestConfiguration
    @ComponentScan(basePackageClasses = AccesoGoogle.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AccesoGoogle.class))
    static class Cliente {
        @Bean
        ClientRegistrationRepository clientes() {
            return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE
                    .getBuilder("google").clientId("cliente-ficticio").clientSecret("solo-prueba")
                    .redirectUri("https://localhost:8443/login/oauth2/code/google").build());
        }
    }
    @Autowired MockMvc mvc;
    @MockitoBean IdentidadExternaRepository identidades;
    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean TokenService tokens;

    @Autowired org.springframework.security.web.FilterChainProxy filtros;

    @Test
    void falloDeBaseEnCallbackNoEntregaCookieNiIdentidad() throws Exception {
        var filtro = filtros.getFilterChains().stream().flatMap(c -> c.getFilters().stream())
                .filter(org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter.class::isInstance)
                .findFirst().orElseThrow();
        org.springframework.security.web.authentication.AuthenticationSuccessHandler handler =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(filtro, "getSuccessHandler");
        var principal = org.mockito.Mockito.mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
        org.mockito.Mockito.when(principal.getSubject()).thenReturn("sub-prueba");
        org.mockito.Mockito.when(identidades.findByProveedorAndSubject(
                dev.reminishub.model.enums.ProveedorExterno.GOOGLE, "sub-prueba"))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("detalle privado"));
        var auth = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                principal, java.util.List.of(), "google");
        var respuesta = new org.springframework.mock.web.MockHttpServletResponse();
        handler.onAuthenticationSuccess(new org.springframework.mock.web.MockHttpServletRequest(), respuesta, auth);
        assertEquals("/login?googleError", respuesta.getRedirectedUrl());
        assertNull(respuesta.getCookie("RH_TOKEN"));
        verifyNoInteractions(tokens);
    }

    @Test
    void iniciaAutorizacionConStateNonceYRetornoHttps() throws Exception {
        var sesion = new org.springframework.mock.web.MockHttpSession();
        String inicio = "https://localhost:8443/oauth2/authorization/google";
        var respuesta = mvc.perform(get(inicio).secure(true).session(sesion))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        var destino = UriComponentsBuilder.fromUriString(respuesta.getRedirectedUrl()).build();
        assertEquals("accounts.google.com", destino.getHost());
        var parametros = destino.getQueryParams();
        assertEquals("select_account", parametros.getFirst("prompt"));
        assertNotNull(parametros.getFirst("state"));
        assertNotNull(parametros.getFirst("nonce"));
        assertEquals("https://localhost:8443/login/oauth2/code/google",
                java.net.URLDecoder.decode(parametros.getFirst("redirect_uri"), java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(get("https://localhost:8443/login/oauth2/code/google").secure(true).session(sesion)
                        .param("state", parametros.getFirst("state")).param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?googleError")).andExpect(cookie().doesNotExist("RH_TOKEN"));
        var reintento = mvc.perform(get(inicio).secure(true).session(sesion))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        var segundo = UriComponentsBuilder.fromUriString(reintento.getRedirectedUrl()).build().getQueryParams();
        assertEquals("select_account", segundo.getFirst("prompt"));
        for (String clave : java.util.List.of("state", "nonce")) {
            assertNotNull(segundo.getFirst(clave));
            assertNotEquals(parametros.getFirst(clave), segundo.getFirst(clave));
        }
    }

    @Test
    void callbackSinSolicitudPreviaNoEmiteCookie() throws Exception {
        mvc.perform(get("https://localhost:8443/login/oauth2/code/google").secure(true)
                        .param("state", "sin-sesion").param("code", "ficticio"))
                .andExpect(redirectedUrl("/login?googleError"))
                .andExpect(cookie().doesNotExist("RH_TOKEN"));
        verifyNoInteractions(identidades, usuarios, tokens);
    }
}
