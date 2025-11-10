package com.itmentorcommunityplatform.gateway.routes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.routes.auth-service.uri=http://localhost:9999"
        }
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewayGeneralIntegrationTest {

    private static final String AUTH_SERVICE_BASE_PATH = "/api/auth";
    private static final String AUTH_SERVICE_INTERNAL_PATH = "/api/auth/internal/test";

    @Autowired
    private WebTestClient webClient;

    @Test
    @DisplayName("Внутренний сервис недоступен -> возвращается статус код 502")
    void downstreamServiceUnavailable_ReturnsStatusCode502() {
        // given
        // when
        webClient.get()
                .uri(AUTH_SERVICE_BASE_PATH)
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Запрос на необрабатываемый путь -> возвращается статус код 404")
    void requestToUnprocessablePath_ReturnsStatusCode404() {
        // given
        // when
        webClient.get()
                .uri("/unknown/test")
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Запрос к внутреннему эндпоинту сервиса -> возвращается статус код 403 Forbidden")
    void requestsToInternalEndpoint_Returns403() {
        // given
        // when
        webClient.get()
                .uri(AUTH_SERVICE_INTERNAL_PATH)
                .exchange()
                // then
                .expectStatus().value(status -> {
                    if (status != HttpStatus.FORBIDDEN.value()) {
                        throw new AssertionError(
                                "Expected " + HttpStatus.FORBIDDEN
                                + " for path " + AUTH_SERVICE_INTERNAL_PATH
                                + ", but got " + HttpStatus.valueOf(status));
                    }
                });
    }
}
