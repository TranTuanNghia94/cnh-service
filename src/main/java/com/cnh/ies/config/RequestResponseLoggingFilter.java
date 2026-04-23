package com.cnh.ies.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet filter that:
 * <ul>
 *   <li>Sets MDC context (requestId, correlationId, userId) for every request so that
 *       ALL downstream log statements automatically carry these fields.</li>
 *   <li>Caches request / response bodies (via {@link ContentCachingRequestWrapper}) so
 *       they can be read for logging without consuming the stream.</li>
 *   <li>Logs request input (method, URI, query, body) and response output
 *       (status, elapsed ms, body) at the end of each request.</li>
 * </ul>
 *
 * Ordered after Spring Security (-100) so {@link SecurityContextHolder} is already
 * populated when this filter runs, allowing the username to be captured.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_SIZE = 3000;

    /** Paths where request body should never be logged (sensitive data). */
    private static final Set<String> SENSITIVE_PATHS = Set.of("/auth/login", "/auth/register", "/auth/refresh-token");

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // ── 1. Build request / correlation IDs ─────────────────────────
        String requestId    = UUID.randomUUID().toString();
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = requestId;
        }

        // ── 2. Populate MDC — all downstream log calls carry these fields
        MDC.put(LoggingInterceptor.REQUEST_ID_MDC_KEY, requestId);
        MDC.put(LoggingInterceptor.CORRELATION_ID_MDC_KEY, correlationId);
        resolveUsername().ifPresent(u -> MDC.put(LoggingInterceptor.USER_ID_MDC_KEY, u));

        // ── 3. Propagate IDs — request attribute read by RequestContext / controllers
        request.setAttribute(LoggingInterceptor.REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Correlation-ID", correlationId);

        // ── 4. Wrap for body caching ────────────────────────────────────
        ContentCachingRequestWrapper  cachedReq = wrapRequest(request);
        ContentCachingResponseWrapper cachedRes = new ContentCachingResponseWrapper(response);

        long startMs = System.currentTimeMillis();
        try {
            chain.doFilter(cachedReq, cachedRes);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startMs;

            // ── 5. Log request + response ──────────────────────────────
            logRequest(cachedReq, requestId);
            logResponse(cachedRes, elapsedMs);

            // MUST forward the cached body to the actual response
            cachedRes.copyBodyToResponse();

            // ── 6. Clean MDC ───────────────────────────────────────────
            MDC.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Request logging
    // ─────────────────────────────────────────────────────────────────────

    private void logRequest(ContentCachingRequestWrapper req, String requestId) {
        String method = req.getMethod();
        String uri    = req.getRequestURI();
        String query  = req.getQueryString();
        String path   = query != null ? uri + "?" + query : uri;

        if (isMultipart(req)) {
            log.info(">>> {} {} [multipart/file-upload, rid={}]", method, path, requestId);
            return;
        }

        String body = readBody(req.getContentAsByteArray(), req.getCharacterEncoding());
        boolean hasSensitivePath = SENSITIVE_PATHS.stream().anyMatch(uri::contains);

        if (hasSensitivePath) {
            log.info(">>> {} {} [body hidden for security, rid={}]", method, path, requestId);
        } else if (body.isBlank()) {
            log.info(">>> {} {} [rid={}]", method, path, requestId);
        } else {
            log.info(">>> {} {} | input: {} [rid={}]", method, path, truncate(body), requestId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Response logging
    // ─────────────────────────────────────────────────────────────────────

    private void logResponse(ContentCachingResponseWrapper res, long elapsedMs) {
        int    status = res.getStatus();
        String body   = readBody(res.getContentAsByteArray(), res.getCharacterEncoding());

        if (status >= 500) {
            log.error("<<< status={} | {}ms | output: {}", status, elapsedMs, truncate(body));
        } else if (status >= 400) {
            log.warn("<<< status={} | {}ms | output: {}", status, elapsedMs, truncate(body));
        } else if (log.isDebugEnabled()) {
            log.debug("<<< status={} | {}ms | output: {}", status, elapsedMs, truncate(body));
        } else {
            log.info("<<< status={} | {}ms", status, elapsedMs);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

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
            // SecurityContext may not be available in all filter paths
        }
        return java.util.Optional.empty();
    }

    private boolean isMultipart(HttpServletRequest req) {
        String ct = req.getContentType();
        return ct != null && ct.startsWith("multipart/");
    }

    private String readBody(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            Charset charset = (encoding != null && !encoding.isBlank())
                    ? Charset.forName(encoding)
                    : StandardCharsets.UTF_8;
            return new String(bytes, charset).strip();
        } catch (Exception e) {
            return "[unreadable body]";
        }
    }

    private String truncate(String s) {
        if (s == null || s.isBlank()) return "";
        return s.length() > MAX_BODY_SIZE
                ? s.substring(0, MAX_BODY_SIZE) + " … [+" + (s.length() - MAX_BODY_SIZE) + " chars]"
                : s;
    }
}
