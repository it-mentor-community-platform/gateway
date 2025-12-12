package com.itmentorcommunityplatform.gateway.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacJjwtDecoderTest {
    private static final String SECRET = "defaultSecretKeyThatIsLongLongLongEnoughForTests";
    private HmacJjwtDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new HmacJjwtDecoder(SECRET);
    }

    @Test
    @DisplayName("Валидный JWT -> корректно декодируется с subject, roles, iat, exp")
    void validJwt_returnsJwtWithCorrectClaims() {
        String token = createValidJwt("user123", List.of("USER"), SECRET);

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("user123");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getIssuedAt()).isBefore(Instant.now());
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("JWT с неверной подписью -> BadJwtException")
    void invalidSignature_throwsBadJwtException() {
        String token = createValidJwt("user123", List.of("USER"),
                "wrongSecretKeyThatIsLongLongLongLongLongLongEnoughForFailTest1331238921");

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    @DisplayName("Пустой токен -> BadJwtException с 'empty or malformed'")
    void emptyToken_throwsBadJwtException() {
        assertThatThrownBy(() -> decoder.decode(""))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("empty or malformed");
    }

    @Test
    @DisplayName("Истёкший JWT -> BadJwtException")
    void expiredToken_throwsBadJwtException() {
        String token = createExpiredJwt(SECRET);

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    @DisplayName("null вместо токена -> BadJwtException")
    void nullToken_throwsBadJwtException() {
        assertThatThrownBy(() -> decoder.decode(null))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("empty or malformed");
    }

    @Test
    @DisplayName("Невалидный формат токена -> BadJwtException")
    void invalidFormatToken_throwsBadJwtException() {
        assertThatThrownBy(() -> decoder.decode("not.a.jwt"))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    @DisplayName("JWT без exp -> BadJwtException")
    void jwtWithoutExp_throwsBadJwtException() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("user")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("malformed");
    }

    private String createValidJwt(String subject, List<String> roles, String secret) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("telegram_username", "test_username")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();
    }

    private String createExpiredJwt(String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject("user")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(1, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();
    }
}