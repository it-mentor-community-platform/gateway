package com.itmentorcommunityplatform.gateway.security.config;

import com.itmentorcommunityplatform.gateway.security.CustomJjwtDecoder;
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

    private final String jwtSecret;

    @Autowired
    public SecurityConfig(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder) throws Exception {
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
                        .authenticationEntryPoint((req, res, authEx) ->
                                sendJsonUnauthorized(res, "Authentication required")))
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
    public JwtDecoder jwtDecoder() {
        return new CustomJjwtDecoder(this.jwtSecret);
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            @Value("${monitoring.prometheus.password}") String rawPassword,
            PasswordEncoder passwordEncoder) {

        UserDetails prometheus = User.withUsername("prometheus")
                .password(passwordEncoder.encode(rawPassword))
                .roles("PROMETHEUS")
                .build();

        return new InMemoryUserDetailsManager(prometheus);
    }

    @Bean
    public PasswordEncoder encoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
