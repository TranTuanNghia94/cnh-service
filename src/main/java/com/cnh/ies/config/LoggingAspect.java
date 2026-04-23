package com.cnh.ies.config;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * AOP aspect for logging service method execution with timing.
 * 
 * Automatically logs:
 * - Method entry with parameters (at DEBUG level)
 * - Method exit with return value and duration (at DEBUG level)
 * - Slow method warnings (if > threshold)
 * - Exceptions (at ERROR level)
 * 
 * All logs include MDC context (requestId, correlationId, userId) from the HTTP filter.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final long SLOW_METHOD_THRESHOLD_MS = 1000;
    private static final int MAX_ARG_LENGTH = 500;
    private static final int MAX_RESULT_LENGTH = 200;

    @Pointcut("within(com.cnh.ies.service..*)")
    public void serviceLayer() {}

    @Pointcut("within(com.cnh.ies.controller..*)")
    public void controllerLayer() {}

    @Pointcut("@annotation(com.cnh.ies.config.Loggable)")
    public void loggableMethods() {}

    /**
     * Log service method execution with timing and parameters.
     */
    @Around("serviceLayer() || loggableMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String fullMethod = className + "." + methodName;

        String requestId = MDC.get("requestId");
        
        if (log.isDebugEnabled()) {
            String args = formatArguments(joinPoint.getArgs(), signature.getParameterNames());
            log.debug(">>> {}.{}({}) [rid={}]", className, methodName, args, requestId);
        }

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            if (elapsedMs > SLOW_METHOD_THRESHOLD_MS) {
                log.warn("SLOW {} completed in {}ms [rid={}]", fullMethod, elapsedMs, requestId);
            } else if (log.isDebugEnabled()) {
                String resultStr = formatResult(result);
                log.debug("<<< {} → {} ({}ms) [rid={}]", fullMethod, resultStr, elapsedMs, requestId);
            }

            return result;
        } catch (Throwable t) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("!!! {} failed after {}ms: {} [rid={}]", 
                fullMethod, elapsedMs, t.getClass().getSimpleName() + ": " + t.getMessage(), requestId);
            throw t;
        }
    }

    private String formatArguments(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            String argValue = formatValue(args[i]);
            
            if (isSensitiveParam(paramName)) {
                sb.append(paramName).append("=[masked]");
            } else {
                sb.append(paramName).append("=").append(argValue);
            }
        }
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        String str;
        if (value.getClass().isArray()) {
            str = Arrays.toString((Object[]) value);
        } else if (value instanceof Iterable) {
            str = "[collection:" + ((java.util.Collection<?>) value).size() + " items]";
        } else {
            str = value.toString();
        }

        if (str.length() > MAX_ARG_LENGTH) {
            return str.substring(0, MAX_ARG_LENGTH) + "...";
        }
        return str;
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        String str;
        if (result instanceof java.util.Collection) {
            str = "[" + ((java.util.Collection<?>) result).size() + " items]";
        } else if (result instanceof java.util.Optional) {
            str = ((java.util.Optional<?>) result).map(v -> "Optional[" + formatValue(v) + "]")
                    .orElse("Optional.empty");
        } else {
            str = result.toString();
        }

        if (str.length() > MAX_RESULT_LENGTH) {
            return str.substring(0, MAX_RESULT_LENGTH) + "...";
        }
        return str;
    }

    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") || lower.contains("secret") 
            || lower.contains("token") || lower.contains("key")
            || lower.contains("credential") || lower.contains("auth");
    }
}
