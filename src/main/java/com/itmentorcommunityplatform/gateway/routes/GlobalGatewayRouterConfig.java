package com.itmentorcommunityplatform.gateway.routes;

import com.itmentorcommunityplatform.gateway.filter.JwtClaimsToHeaderFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GlobalGatewayRouterConfig {

    private final JwtClaimsToHeaderFilter jwtClaimsFilter;

    @Autowired
    public GlobalGatewayRouterConfig(JwtClaimsToHeaderFilter jwtFilter) {
        this.jwtClaimsFilter = jwtFilter;
    }

    @Bean
    @Primary
    public RouterFunction<ServerResponse> globalRouterFunction(
            RouterFunction<ServerResponse> defaultRouter) {

        HandlerFilterFunction<ServerResponse, ServerResponse> filter =
                HandlerFilterFunction.ofRequestProcessor(jwtClaimsFilter);

        return RouterFunctions.route()
                .nest(request -> true, builder -> builder
                        .add(defaultRouter)
                        .filter(filter)
                )
                .build();
    }
}
