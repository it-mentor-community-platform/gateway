package com.itmentorcommunityplatform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class GatewayRestClientConfig {

    @Bean
    public HttpClient gatewayHttpClient(@Value("${app.http-client.connect-timeout:2000}") long connectTimeoutMs) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @Bean
    public RestClientCustomizer timeoutRestClientCustomizer(
            HttpClient httpClient,
            @Value("${app.http-client.read-timeout:8000}") long readTimeoutMs) {
        return builder -> {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            builder.requestFactory(factory);
        };
    }
}
