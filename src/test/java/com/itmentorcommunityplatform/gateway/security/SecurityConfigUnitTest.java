package com.itmentorcommunityplatform.gateway.security;

import com.itmentorcommunityplatform.gateway.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, SecurityConfigUnitTest.TestSecurityDummyController.class})
@WebMvcTest
@AutoConfigureMockMvc
@DisplayName("SecurityConfig тестирование правил доступа")
@ActiveProfiles("test")
class SecurityConfigUnitTest {

    private static final String AUTH_INTERNAL_URL = "/api/auth/internal/test";
    private static final String MINIMAL_INTERNAL_URL = "/api/auth/internal";
    private static final String NO_SERVICE_SEGMENT_INTERNAL_URL = "/api/internal/test";
    private static final String AUTH_LOGIN_URL = "/api/auth/login";
    private static final String JWT_PROTECTED_URL = "/api/unknown/path";
    private static final String PUBLIC_WITH_INTERNAL_URL = "/api/auth/login/internal/secret";

    private static final String PROMETHEUS_URL = "/actuator/prometheus";
    private static final String HEALTH_URL = "/actuator/health";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @RestController
    public static class TestSecurityDummyController {
        @GetMapping(AUTH_INTERNAL_URL) public String internalAuth() {return "ok";}
        @GetMapping(NO_SERVICE_SEGMENT_INTERNAL_URL) public String noSegment() { return "ok"; }
        @GetMapping(MINIMAL_INTERNAL_URL) public String minimalInternal() { return "ok"; }
        @GetMapping(AUTH_LOGIN_URL) public String login() {return "ok";}
        @GetMapping(JWT_PROTECTED_URL) public String someService() {return "ok";}
        @GetMapping(PUBLIC_WITH_INTERNAL_URL) public String publicWithInternal() { return "ok"; }

        @GetMapping(PROMETHEUS_URL) public String prometheus() {
            return "# HELP test_metric Example metric\n# TYPE test_metric gauge\ntest_metric 1.0\n";}
        @GetMapping(HEALTH_URL) public String health() {return "{\"status\":\"UP\"}";}
    }

    @Test
    @DisplayName("Запрос к внутренним эндпоинтам сервисов с валидным jwt токеном -> 403 Forbidden")
    void requestsToInternalEndpointsWithValidJwt_Returns4xx() throws Exception {
        mockMvc.perform(get(AUTH_INTERNAL_URL).with(jwt()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Минимальный internal путь (без подпути) с валидным jwt токеном -> 403 Forbidden")
    void minimalInternalPath_Returns4xx() throws Exception {
        mockMvc.perform(get(MINIMAL_INTERNAL_URL).with(jwt()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Запросы к сервису авторизации без JWT -> 200 OK")
    void publicAuthPathWithoutJwt_Returns200() throws Exception {
        mockMvc.perform(get(AUTH_LOGIN_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Запрос на защищенный путь с валидным jwt -> 200 OK (нет ограничений)")
    void requestsToUnknownEndpointsWithValidJwt_Returns200() throws Exception {
        mockMvc.perform(get(JWT_PROTECTED_URL).with(jwt()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Запрос на защищенный путь без jwt -> 401 Unauthorized")
    void requestsToUnknownEndpointsWithoutJwt_Returns200() throws Exception {
        mockMvc.perform(get(JWT_PROTECTED_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("internal внутри публичного пути -> 200 OK")
    void internalInPublicPath_Returns200() throws Exception {
        mockMvc.perform(get(PUBLIC_WITH_INTERNAL_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Запрос на внутренний путь без сегмента сервиса с валидным jwt -> 200 OK")
    void noServiceSegmentInternalPathsWithValidJwt_Returns200() throws Exception {
        mockMvc.perform(get(NO_SERVICE_SEGMENT_INTERNAL_URL).with(jwt()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/actuator/prometheus без basic авторизации -> 401 Unauthorized")
    void prometheusWithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("Basic")));
    }

    @Test
    @DisplayName("/actuator/prometheus с неправильными credentials -> 401 Unauthorized")
    void prometheusWithWrongCredentials_Returns401() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL)
                        .with(httpBasic("prometheus", "wrong_password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/actuator/prometheus с правильными credentials -> 200 OK")
    void prometheusWithCorrectCredentials_Returns200() throws Exception {
        mockMvc.perform(get(PROMETHEUS_URL)
                        .with(httpBasic("prometheus", "password")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test_metric")));
    }

    @Test
    @DisplayName("/actuator/health доступен без авторизации -> 200 OK")
    void healthEndpointIsPublic_Returns200() throws Exception {
        mockMvc.perform(get(HEALTH_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
