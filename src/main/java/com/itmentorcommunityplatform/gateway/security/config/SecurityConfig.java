package com.itmentorcommunityplatform.gateway.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_SERVICE_INTERNAL_PATTERN = "/api/{service:^(?!$).+}/internal/**";

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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(API_SERVICE_INTERNAL_PATTERN).denyAll()
                        .anyRequest().permitAll()
                )
                .build();
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
