package com.itmentorcommunityplatform.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpTimeoutException;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalGatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalGatewayExceptionHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        log.error("Resource access error during request to downstream service", ex);

        boolean isTimeout = isContainsHttpTimeout(ex.getCause());

        HttpStatus status = isTimeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        String errorMessage = isTimeout ? "Gateway timeout" : "Bad Gateway";
        String detailedMessage = isTimeout ? "Connection to target service is timed out" :
                                             "Target service is unavailable or connection failed";

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", errorMessage,
                "message", detailedMessage
        );

        return ResponseEntity.status(status).body(body);
    }

    private boolean isContainsHttpTimeout(Throwable cause) {
        while (cause != null) {
            if (cause instanceof HttpTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
