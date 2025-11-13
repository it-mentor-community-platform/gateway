package com.itmentorcommunityplatform.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class CustomJjwtDecoder implements JwtDecoder {

    private final SecretKey key;

    public CustomJjwtDecoder(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(claims.getHeader()))
                    .claims(c -> c.putAll(claims.getPayload()))
                    .build();

        } catch (io.jsonwebtoken.JwtException e) {
            throw new BadJwtException("Invalid JWT token", e);
        } catch (IllegalArgumentException e) {
            throw new BadJwtException("JWT token is empty or malformed", e);
        }
    }
}
