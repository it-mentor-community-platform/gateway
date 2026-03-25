package com.itmentorcommunityplatform.gateway.filter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;
import java.util.function.Function;

@Component
public class JwtClaimsToHeaderFilter implements Function<ServerRequest, ServerRequest> {

    private static final String USER_ID_HEADER_NAME = "X-Telegram-User-Id";
    private static final String USER_ROLES_HEADER_NAME = "X-User-Roles";
    private static final String USERNAME_HEADER_NAME = "X-Telegram-Username";
    private static final String ROLES_CLAIM_NAME = "roles";
    private static final String USERNAME_CLAIM_NAME = "telegram_username";

    @Override
    public ServerRequest apply(ServerRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();

            return ServerRequest.from(request)
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER_NAME);
                        headers.remove(USER_ROLES_HEADER_NAME);
                        headers.remove(USERNAME_HEADER_NAME);

                        headers.set(USER_ID_HEADER_NAME, jwt.getSubject());

                        String telegramUsername = jwt.getClaimAsString(USERNAME_CLAIM_NAME);
                        if (telegramUsername != null) {
                            headers.set(USERNAME_HEADER_NAME, telegramUsername);
                        }

                        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM_NAME);
                        if (roles != null && !roles.isEmpty()) {
                            headers.set(USER_ROLES_HEADER_NAME, String.join(",", roles));
                        }
                    })
                    .build();
        }
        return request;
    }
}
