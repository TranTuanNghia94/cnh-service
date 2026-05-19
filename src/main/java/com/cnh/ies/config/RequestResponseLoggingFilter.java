package com.cnh.ies.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP request/response logging with compact INFO lines and full JSON detail at DEBUG.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUEST_BODY_SIZE = 2000;
    private static final int MAX_RESPONSE_BODY_SIZE = 8000;

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/refresh-token", "/auth/change-password");

    private static final Set<String> IMPORTANT_REQUEST_HEADERS = Set.of(
            "Content-Type", "Accept", "User-Agent", "X-Forwarded-For", "X-Real-IP", "Origin", "Referer");

    private static final Set<String> SKIP_RESPONSE_BODY_CONTENT_TYPES = Set.of(
            "application/octet-stream", "image/", "video/", "audio/", "application/pdf", "text/event-stream");

    private final ObjectMapper objectMapper;

    @Value("${logging.http.include-headers:true}")
    private boolean includeHeaders;

    /** Full response body in DEBUG logs (recommended: false for INFO readability). */
    @Value("${logging.http.include-response-body:false}")
    private boolean includeResponseBodyInDebug;

    public RequestResponseLoggingFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = requestId;
        }

        MDC.put(LoggingInterceptor.REQUEST_ID_MDC_KEY, requestId);
        MDC.put(LoggingInterceptor.CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put("method", request.getMethod());
        MDC.put("uri", uri);
        resolveUsername().ifPresent(u -> MDC.put(LoggingInterceptor.USER_ID_MDC_KEY, u));

        request.setAttribute(LoggingInterceptor.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Correlation-ID", correlationId);

        ContentCachingRequestWrapper cachedReq = wrapRequest(request);
        ContentCachingResponseWrapper cachedRes = new ContentCachingResponseWrapper(response);

        long startNanos = System.nanoTime();
        Throwable error = null;

        try {
            chain.doFilter(cachedReq, cachedRes);
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            MDC.put("elapsedMs", String.valueOf(elapsedMs));
            MDC.put("status", String.valueOf(cachedRes.getStatus()));

            logRequestResponse(cachedReq, cachedRes, elapsedMs, error);

            cachedRes.copyBodyToResponse();
            MDC.clear();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper req,
                                    ContentCachingResponseWrapper res,
                                    long elapsedMs,
                                    Throwable error) {
        String method = req.getMethod();
        String uri = req.getRequestURI();
        String query = req.getQueryString();
        String fullPath = query != null ? uri + "?" + query : uri;
        int status = res.getStatus();
        boolean isSensitive = SENSITIVE_PATHS.stream().anyMatch(uri::contains);
        String clientIp = getClientIp(req);
        String errorText = error != null
                ? error.getClass().getSimpleName() + ": " + error.getMessage()
                : null;

        Map<String, String> requestHeaders = includeHeaders ? extractImportantHeaders(req) : Map.of();

        String requestBodyRaw = null;
        if (!isMultipart(req) && !isSensitive) {
            requestBodyRaw = readBody(req.getContentAsByteArray(), req.getCharacterEncoding(), MAX_REQUEST_BODY_SIZE);
        } else if (isMultipart(req)) {
            requestBodyRaw = "[multipart/file-upload]";
        } else if (isSensitive) {
            requestBodyRaw = "[hidden:sensitive]";
        }
        String requestBodyCompact = requestBodyRaw != null ? compactJson(requestBodyRaw) : null;

        String responseBodyRaw = null;
        if (!shouldSkipResponseBody(res)) {
            responseBodyRaw = readBody(res.getContentAsByteArray(), res.getCharacterEncoding(), MAX_RESPONSE_BODY_SIZE);
        } else {
            responseBodyRaw = responseBodyPlaceholder(res);
        }
        String responseBodyCompact = responseBodyRaw != null ? compactJson(responseBodyRaw) : null;

        Object responseSummary = HttpLogFormatter.summarizeResponseBody(objectMapper, responseBodyCompact);
        String requestPreview = requestBodyCompact != null
                ? HttpLogFormatter.preview(requestBodyCompact)
                : null;

        String infoLine = HttpLogFormatter.formatInfoLine(
                objectMapper, method, fullPath, status, elapsedMs, clientIp,
                requestPreview, responseSummary, errorText);

        if (error != null || status >= 500) {
            log.error(infoLine);
        } else if (status >= 400) {
            log.warn(infoLine);
        } else {
            log.info(infoLine);
        }

        if (log.isDebugEnabled()) {
            String debugResponseBody = includeResponseBodyInDebug
                    ? responseBodyCompact
                    : (responseSummary != null ? toJsonSafe(responseSummary) : null);
            Map<String, Object> detail = HttpLogFormatter.buildDetail(
                    objectMapper,
                    method,
                    fullPath,
                    status,
                    elapsedMs,
                    requestHeaders.isEmpty() ? null : requestHeaders,
                    requestBodyCompact,
                    debugResponseBody,
                    clientIp,
                    errorText);
            try {
                log.debug("HTTP detail {}", objectMapper.writeValueAsString(detail));
            } catch (JsonProcessingException e) {
                log.debug("HTTP detail {}", detail);
            }
        }
    }

    private String responseBodyPlaceholder(ContentCachingResponseWrapper res) {
        String contentType = res.getContentType();
        if (contentType != null && contentType.contains("text/event-stream")) {
            return "[sse/stream]";
        }
        if (contentType != null && SKIP_RESPONSE_BODY_CONTENT_TYPES.stream().anyMatch(contentType::contains)) {
            return "[binary/stream:" + contentType + "]";
        }
        return null;
    }

    private String toJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String compactJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            return json.replaceAll("\\s+", " ").trim();
        }
    }

    private Map<String, String> extractImportantHeaders(HttpServletRequest req) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (IMPORTANT_REQUEST_HEADERS.stream().anyMatch(h -> h.equalsIgnoreCase(name))) {
                String value = req.getHeader(name);
                if (name.equalsIgnoreCase("Authorization")) {
                    value = maskAuthHeader(value);
                }
                headers.put(name, value);
            }
        }
        return headers;
    }

    private String maskAuthHeader(String value) {
        if (value == null) {
            return null;
        }
        if (value.toLowerCase().startsWith("bearer ") && value.length() > 20) {
            return "Bearer " + value.substring(7, 15) + "...";
        }
        return "[masked]";
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return req.getRemoteAddr();
    }

    private boolean shouldSkipLogging(String uri) {
        return uri.contains("/actuator") || uri.contains("/health") || uri.contains("/swagger")
                || uri.contains("/v3/api-docs") || uri.endsWith("/favicon.ico");
    }

    private boolean shouldSkipResponseBody(ContentCachingResponseWrapper res) {
        String contentType = res.getContentType();
        if (contentType == null) {
            return false;
        }
        return SKIP_RESPONSE_BODY_CONTENT_TYPES.stream().anyMatch(contentType::contains);
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        return (request instanceof ContentCachingRequestWrapper cached)
                ? cached
                : new ContentCachingRequestWrapper(request);
    }

    private java.util.Optional<String> resolveUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return java.util.Optional.ofNullable(auth.getName());
            }
        } catch (Exception ignored) {
            // ignore
        }
        return java.util.Optional.empty();
    }

    private boolean isMultipart(HttpServletRequest req) {
        String ct = req.getContentType();
        return ct != null && ct.startsWith("multipart/");
    }

    private String readBody(byte[] bytes, String encoding, int maxSize) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            Charset charset = (encoding != null && !encoding.isBlank())
                    ? Charset.forName(encoding)
                    : StandardCharsets.UTF_8;
            String body = new String(bytes, charset).strip();
            if (body.length() > maxSize) {
                return body.substring(0, maxSize) + "...[truncated:" + (body.length() - maxSize) + "chars]";
            }
            return body;
        } catch (Exception e) {
            return "[unreadable]";
        }
    }
}
