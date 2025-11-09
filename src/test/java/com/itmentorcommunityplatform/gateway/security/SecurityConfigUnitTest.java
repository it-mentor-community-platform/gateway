package com.itmentorcommunityplatform.gateway.security;

import com.itmentorcommunityplatform.gateway.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, SecurityConfigUnitTest.TestSecurityDummyController.class})
@WebMvcTest
@AutoConfigureMockMvc
@DisplayName("SecurityConfig тестирование правил доступа")
class SecurityConfigUnitTest {

    private static final String AUTH_INTERNAL_URL = "/api/auth/internal/test";
    private static final String MINIMAL_INTERNAL_URL = "/api/auth/internal";
    private static final String NO_SERVICE_SEGMENT_URL = "/api/internal/test";
    private static final String AUTH_LOGIN_URL = "/api/auth/login";
    private static final String UNKNOWN_URL = "/unknown/path";
    private static final String PUBLIC_WITH_INTERNAL_URL = "/api/auth/login/internal/secret";

    @Autowired
    private MockMvc mockMvc;

    @RestController
    public static class TestSecurityDummyController {
        @GetMapping(AUTH_INTERNAL_URL) public String internalAuth() {return "ok";}
        @GetMapping(NO_SERVICE_SEGMENT_URL) public String noSegment() { return "ok"; }
        @GetMapping(MINIMAL_INTERNAL_URL) public String minimalInternal() { return "ok"; }
        @GetMapping(AUTH_LOGIN_URL) public String login() {return "ok";}
        @GetMapping(UNKNOWN_URL) public String unknown() {return "ok";}
        @GetMapping(PUBLIC_WITH_INTERNAL_URL) public String publicWithInternal() { return "ok"; }
    }

    @Test
    @DisplayName("Запрос к внутренним эндпоинтам сервисов -> возвращается статус код 403 Forbidden")
    void requestsToInternalEndpoints_Returns403() throws Exception {
        mockMvc.perform(get(AUTH_INTERNAL_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Минимальный internal путь (без подпути) → 403")
    void minimalInternalPath_Returns403() throws Exception {
        mockMvc.perform(get(MINIMAL_INTERNAL_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Запрос к публичным эндпоинтам сервисов -> возвращается статус код 200 OK")
    void requestsToPublicEndpoints_Returns200() throws Exception {
        mockMvc.perform(get(AUTH_LOGIN_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Неизвестный путь → 200 OK (нет ограничений)")
    void requestsToUnknownEndpoints_Returns200() throws Exception {
        mockMvc.perform(get(UNKNOWN_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("internal внутри публичного пути -> 200 OK")
    void internalInPublicPath_Returns200() throws Exception {
        mockMvc.perform(get(PUBLIC_WITH_INTERNAL_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("путь без сегмента сервиса -> 200 OK")
    void noServiceSegmentPaths_Returns200() throws Exception {
        mockMvc.perform(get(NO_SERVICE_SEGMENT_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
