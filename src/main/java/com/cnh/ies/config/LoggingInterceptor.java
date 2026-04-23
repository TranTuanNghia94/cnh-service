package com.cnh.ies.config;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Holds the shared MDC / request-attribute key constants used by
 * {@link RequestResponseLoggingFilter} and {@link com.cnh.ies.util.RequestContext}.
 *
 * MDC management and request/response logging are performed by
 * {@link RequestResponseLoggingFilter}. This interceptor is kept solely as
 * a constant-holder and registration hook; its handler methods are intentional no-ops.
 */
public class LoggingInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER      = "X-Request-ID";
    public static final String USERNAME_ATTRIBUTE      = "username";
    public static final String REQUEST_ID_ATTRIBUTE    = "requestId";
    public static final String REQUEST_ID_MDC_KEY      = "requestId";
    public static final String USER_ID_MDC_KEY         = "userId";
    public static final String CORRELATION_ID_MDC_KEY  = "correlationId";

    @Override
    @SuppressWarnings("null")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return true;
    }
}
