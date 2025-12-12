package com.itmentorcommunityplatform.gateway.routes;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class GatewayGeneralIntegrationTest {

    private static final String AUTH_SERVICE_BASE_PATH = "/api/auth";
    private static final String AUTH_SERVICE_INTERNAL_PATH = "/api/auth/internal/test";

    private static final String NOT_ACTIVE_SERVICE_PATH = "/api/not-active-service/endpoint";

    private static final String TEST_SERVICE_PATH = "/api/test/endpoint";
    private static final String TEST_SERVICE_INTERNAL_PATH = "/api/test/internal";

    private static final String ACCESS_TOKEN_HEADER_NAME = "X-Access-Token";
    private static final String USER_ID_HEADER_NAME = "X-Telegram-User-Id";
    private static final String USER_ROLES_HEADER_NAME = "X-User-Roles";
    private static final String USERNAME_HEADER_NAME = "X-Telegram-Username";
    private static final String ROLES_CLAIM_NAME = "roles";
    private static final String USERNAME_CLAIM_NAME = "telegram_username";
    private static final String TEST_USER = "user_telegram_id";
    private static final String TEST_USERNAME = "zhukovsd";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_STUDENT = "STUDENT";
    private final String jwtSecret;
    private final String validJwtTestUserAdminStudent;

    @Autowired
    private WebTestClient webClient;

    @RegisterExtension
    static WireMockExtension downstreamServiceMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    public GatewayGeneralIntegrationTest(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
        this.validJwtTestUserAdminStudent = createValidJwt(TEST_USER, TEST_USERNAME, List.of(ROLE_ADMIN, ROLE_STUDENT));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes.test-service.uri", () ->
                "http://localhost:" + downstreamServiceMock.getPort());
    }
    
    @Test
    @DisplayName("Внутренний публичный сервис недоступен -> возвращается статус код 502")
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
    @DisplayName("Запрос на защищенный путь без jwt токена -> возвращается статус код 401")
    void requestToSecuredPath_ReturnsStatusCode401() {
        // given
        // when
        webClient.get()
                .uri(TEST_SERVICE_PATH)
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Запрос к внутреннему эндпоинту сервиса без jwt токена -> возвращается статус код 401")
    void requestsToInternalEndpoint_Returns401() {
        // given
        // when
        webClient.get()
                .uri(AUTH_SERVICE_INTERNAL_PATH)
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Валидный JWT -> downstream получает заголовки с user id и roles")
    void validJwt_ClaimsAreForwardedToDownstream() {
        // given
        downstreamServiceMock.stubFor(get(TEST_SERVICE_PATH)
                .willReturn(okJson("""
                        {
                          "test": "ok"
                        }
                        """)));
        // when
        webClient.get()
                .uri(TEST_SERVICE_PATH)
                .header(ACCESS_TOKEN_HEADER_NAME, validJwtTestUserAdminStudent)
                .exchange()
                // then
                .expectStatus().isOk()
                .expectBody().jsonPath("$.test").isEqualTo("ok");

        downstreamServiceMock.verify(
                getRequestedFor(urlEqualTo(TEST_SERVICE_PATH))
                        .withHeader(USER_ID_HEADER_NAME, equalTo(TEST_USER))
                        .withHeader(USER_ROLES_HEADER_NAME, equalTo(ROLE_ADMIN + "," + ROLE_STUDENT))
                        .withHeader(USERNAME_HEADER_NAME, equalTo(TEST_USERNAME))
        );
    }

    @Test
    @DisplayName("Защищенный сервис недоступен с валидным JWT -> 502 Bad Gateway")
    void validJwt_downstreamSecuredServiceUnavailable_ReturnsStatusCode502() {
        // given
        // when
        webClient.get()
                .uri(NOT_ACTIVE_SERVICE_PATH)
                .header(ACCESS_TOKEN_HEADER_NAME, validJwtTestUserAdminStudent)
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
                .expectBody(String.class);
    }

    @Test
    @DisplayName("Превышен интервал ожидания ответа от downstream сервиса  с валидным JWT -> 504 Gateway Timeout")
    void validJwt_downstreamSecuredServiceTimedOut_ReturnsStatusCode504() {
        // given
        downstreamServiceMock.stubFor(get(TEST_SERVICE_PATH + "/slow")
                .willReturn(okJson("""
                        {
                          "test": "ok"
                        }
                        """)
                        .withFixedDelay(2000)));
        // when
        webClient.get()
                .uri(TEST_SERVICE_PATH + "/slow")
                .header(ACCESS_TOKEN_HEADER_NAME, validJwtTestUserAdminStudent)
                .exchange()
                // then
                .expectStatus().isEqualTo(HttpStatus.GATEWAY_TIMEOUT);

        downstreamServiceMock.verify(
                getRequestedFor(urlEqualTo(TEST_SERVICE_PATH + "/slow"))
        );
    }

    @Test
    @DisplayName("Валидный JWT + internal путь -> 403 Forbidden")
    void validJwt_InternalPath_Returns403() {
        // given
        // when
        webClient.get()
                .uri(TEST_SERVICE_INTERNAL_PATH + "/secret")
                .header(ACCESS_TOKEN_HEADER_NAME, validJwtTestUserAdminStudent)
                .exchange()
                // then
                .expectStatus().isForbidden();

        downstreamServiceMock.verify(0,
                getRequestedFor(urlMatching(TEST_SERVICE_INTERNAL_PATH + "/.*"))
        );
    }

    private String createValidJwt(String userId, String username, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim(ROLES_CLAIM_NAME, roles)
                .claim(USERNAME_CLAIM_NAME, username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000L)) // 1 hour
                .signWith(key)
                .compact();
    }
}
