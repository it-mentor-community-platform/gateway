package com.itmentorcommunityplatform.gateway.security.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_SERVICE_INTERNAL_PATTERN = "/api/{service:^(?!$).+}/internal/**";
    private static final String ACCESS_TOKEN_HEADER_NAME = "X-Access-Token";

    private final JwtDecoder jwtDecoder;

    @Autowired
    public SecurityConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain basicAuthInMemorySecurityFilterChain(HttpSecurity http,
                                                           InMemoryUserDetailsManager inMemoryUds) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(inMemoryUds)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/prometheus").hasRole("PROMETHEUS")
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain telegramBotAdapterSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/telegram-bot-adapter/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(API_SERVICE_INTERNAL_PATTERN).denyAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .bearerTokenResolver(request -> request
                                .getHeader(ACCESS_TOKEN_HEADER_NAME))
                        .authenticationEntryPoint((req, res, authEx) ->
                                sendJsonUnauthorized(res, "Authentication required: " + authEx.getMessage())))
                .build();
    }

    private void sendJsonUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("""
        {
            "message": "%s"
        }
        """.formatted(message));
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            @Value("${monitoring.prometheus.password}") String rawPassword,
            @Value("${telegram-bot-adapter.auth.username}") String adapterUsername,
            @Value("${telegram-bot-adapter.auth.password}") String adapterPassword,
            PasswordEncoder passwordEncoder) {

        UserDetails prometheus = User.withUsername("prometheus")
                .password(passwordEncoder.encode(rawPassword))
                .roles("PROMETHEUS")
                .build();

        UserDetails telegramAdapter = User.withUsername(adapterUsername)
                .password(passwordEncoder.encode(adapterPassword))
                .roles("TELEGRAM_ADAPTER")
                .build();

        return new InMemoryUserDetailsManager(prometheus, telegramAdapter);
    }

    @Bean
    public PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
