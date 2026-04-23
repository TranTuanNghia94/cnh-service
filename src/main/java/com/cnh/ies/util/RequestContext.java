package com.cnh.ies.util;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cnh.ies.config.LoggingInterceptor;

/**
 * Utility class for accessing request context information.
 * 
 * All context values are set by {@link com.cnh.ies.config.RequestResponseLoggingFilter}
 * and are available throughout the request lifecycle.
 * 
 * MDC values available:
 * - requestId: unique identifier for this request
 * - correlationId: propagated from caller or same as requestId
 * - userId: authenticated user (if available)
 * - method: HTTP method
 * - uri: request URI
 */
public class RequestContext {

    private RequestContext() {}

    /**
     * Get the current request ID from MDC (preferred) or request attribute.
     * Use this instead of generating your own UUID in controllers/services.
     */
    public static String getRequestId() {
        String rid = MDC.get(LoggingInterceptor.REQUEST_ID_MDC_KEY);
        if (rid != null) {
            return rid;
        }
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return (String) attributes.getRequest().getAttribute(LoggingInterceptor.REQUEST_ID_ATTRIBUTE);
        }
        return null;
    }

    /**
     * Get the correlation ID for distributed tracing.
     */
    public static String getCorrelationId() {
        return MDC.get(LoggingInterceptor.CORRELATION_ID_MDC_KEY);
    }

    /**
     * Get the current authenticated user ID from MDC.
     */
    public static String getUserId() {
        return MDC.get(LoggingInterceptor.USER_ID_MDC_KEY);
    }

    /**
     * Get the current HTTP request.
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Get the current authenticated username from SecurityContext.
     */
    public static String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Check if there is an active request context.
     */
    public static boolean hasRequestContext() {
        return RequestContextHolder.getRequestAttributes() != null;
    }

    @Deprecated
    public static String getCurrentRequestId() {
        return getRequestId();
    }

    @Deprecated
    public static void setCurrentRequestId(String requestId) {
    }

    @Deprecated
    public static void removeCurrentRequestId(String requestId) {
    }
}
