package com.itmentorcommunityplatform.gateway.routes;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewayAuthServiceIntegrationTest {

    private static final String AUTH_SERVICE_BASE_PATH = "/api/auth";

    @Autowired
    private WebTestClient webClient;

    @RegisterExtension
    static WireMockExtension authServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes.auth-service.uri", () ->
                "http://localhost:" + authServiceMock.getPort());
    }

    @Test
    @DisplayName("Запрос по валидному пути к AuthService -> успешное проксирование")
    void requestToValidAuthServicePath_SuccessfulProxyingToAuthService() {
        // given
        authServiceMock.stubFor(WireMock.get(AUTH_SERVICE_BASE_PATH + "/test")
                .willReturn(WireMock.okJson("""
                        {
                          "auth": "ok"
                        }
                        """)));
        // when
        webClient.get()
                .uri(AUTH_SERVICE_BASE_PATH + "/test")
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody().json("""
                        {
                          "auth": "ok"
                        }
                        """);
        authServiceMock.verify(
                WireMock.getRequestedFor(WireMock.urlEqualTo(AUTH_SERVICE_BASE_PATH + "/test"))
        );
    }

    @Test
    @DisplayName("Запрос по валидному пути к AuthService -> передаются кастомные заголовки")
    void requestToValidAuthServicePath_ForwardsCustomHeaders() {
        // given
        authServiceMock.stubFor(WireMock.get(AUTH_SERVICE_BASE_PATH + "/headers")
                .withHeader("X-Request-Custom-Header", WireMock.equalTo("header value"))
                .willReturn(WireMock.okJson("""
                        {
                          "header": "ok"
                        }
                        """)));
        // when
        webClient.get().uri(AUTH_SERVICE_BASE_PATH + "/headers")
                .header("X-Request-Custom-Header", "header value")
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody().json("""
                        {
                          "header": "ok"
                        }
                        """);
        authServiceMock.verify(
                WireMock.getRequestedFor(WireMock.urlEqualTo(AUTH_SERVICE_BASE_PATH + "/headers"))
        );
    }

    @Test
    @DisplayName("Запрос к AuthService по неизвестному пути -> возвращается статус код 404, без подмены")
    void requestToAuthServiceByUnknownPath_ReturnsStatusCode404WithoutSubstitute() {
        // given
        authServiceMock.stubFor(WireMock.get(AUTH_SERVICE_BASE_PATH + "/notfound")
                .willReturn(WireMock.notFound()));
        // when
        webClient.get()
                .uri(AUTH_SERVICE_BASE_PATH + "/notfound")
                .exchange()
                // then
                .expectStatus().isNotFound();
        authServiceMock.verify(
                WireMock.getRequestedFor(WireMock.urlEqualTo(AUTH_SERVICE_BASE_PATH + "/notfound"))
        );
    }

    @Test
    @DisplayName("AuthService не отвечает до истечения таймаута запроса -> возвращается статус код 504")
    void authServiceDoesNotRespondBeforeRequestTimesOut_ReturnsStatusCode504() {
        // given
        authServiceMock.stubFor(WireMock.get(AUTH_SERVICE_BASE_PATH + "/slow")
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(2000)));
        // when
        webClient.get()
                .uri(AUTH_SERVICE_BASE_PATH + "/slow")
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        authServiceMock.verify(
                WireMock.getRequestedFor(WireMock.urlEqualTo(AUTH_SERVICE_BASE_PATH + "/slow"))
        );
    }
}
