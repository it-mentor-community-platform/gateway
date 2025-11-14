package com.itmentorcommunityplatform.gateway.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class HmacJjwtDecoder implements JwtDecoder {

    private final SecretKey key;

    public HmacJjwtDecoder(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();

            Instant issuedAt = Optional.ofNullable(claims.getIssuedAt())
                    .map(Date::toInstant)
                    .orElseThrow(() -> new IllegalArgumentException("Missing iat"));

            Instant expiresAt = Optional.ofNullable(claims.getExpiration())
                    .map(Date::toInstant)
                    .orElseThrow(() -> new IllegalArgumentException("Missing exp"));

            return Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(jws.getHeader()))
                    .claims(c -> c.putAll(claims))
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .build();

        } catch (io.jsonwebtoken.JwtException e) {
            throw new BadJwtException("Invalid JWT token", e);
        } catch (IllegalArgumentException e) {
            throw new BadJwtException("JWT token is empty or malformed", e);
        }
    }
}
