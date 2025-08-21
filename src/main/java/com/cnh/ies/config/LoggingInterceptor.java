package com.cnh.ies.config;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoggingInterceptor implements HandlerInterceptor {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String USERNAME_ATTRIBUTE = "username";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate requestId
        String requestId = UUID.randomUUID().toString();
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = requestId; // Use requestId as correlationId if not provided
        }

        // Store in request attributes for use in controllers and services
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

        // Store in MDC for logging
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Extract user ID from JWT if available (you can implement this based on your auth)
        String userId = extractUserIdFromRequest(request);
        if (userId != null) {
            MDC.put(USER_ID_MDC_KEY, userId);
        }

        // Add to response headers
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader("X-Correlation-ID", correlationId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            @SuppressWarnings("null") Exception ex) {
        // Clean up MDC
        MDC.remove(REQUEST_ID_MDC_KEY);
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
        MDC.remove("username");
    }
    
    private String extractUserIdFromRequest(HttpServletRequest request) {
        // Extract user ID from JWT token or other authentication mechanism
        // This is a placeholder - implement based on your authentication strategy
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // TODO: Decode JWT and extract user ID
            // For now, return null
            return null;
        }
        return null;
    }
}