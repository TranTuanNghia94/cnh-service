package com.cnh.ies.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds compact, readable HTTP log payloads for INFO/WARN and full detail for DEBUG.
 */
final class HttpLogFormatter {

    private static final int PREVIEW_MAX_CHARS = 240;

    private HttpLogFormatter() {}

    static Map<String, Object> buildDetail(
            ObjectMapper objectMapper,
            String method,
            String fullPath,
            int status,
            long elapsedMs,
            Map<String, String> requestHeaders,
            String requestBody,
            String responseBody,
            String clientIp,
            String error) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("type", "HTTP");
        logData.put("method", method);
        logData.put("path", fullPath);
        logData.put("status", status);
        logData.put("elapsedMs", elapsedMs);
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            logData.put("requestHeaders", requestHeaders);
        }
        if (requestBody != null && !requestBody.isBlank()) {
            logData.put("requestBody", requestBody);
        }
        if (responseBody != null && !responseBody.isBlank()) {
            logData.put("responseBody", responseBody);
        }
        if (clientIp != null) {
            logData.put("clientIp", clientIp);
        }
        if (error != null) {
            logData.put("error", error);
        }
        return logData;
    }

    static String formatInfoLine(
            ObjectMapper objectMapper,
            String method,
            String fullPath,
            int status,
            long elapsedMs,
            String clientIp,
            String requestBodyPreview,
            Object responseSummary,
            String error) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("HTTP ").append(method).append(' ').append(fullPath)
                .append(" → ").append(status)
                .append(" (").append(elapsedMs).append("ms)");
        if (clientIp != null && !clientIp.isBlank()) {
            sb.append(" | ip=").append(clientIp);
        }
        if (requestBodyPreview != null && !requestBodyPreview.isBlank()) {
            sb.append(" | req=").append(requestBodyPreview);
        }
        if (responseSummary != null) {
            sb.append(" | res=").append(toJson(objectMapper, responseSummary));
        }
        if (error != null) {
            sb.append(" | err=").append(error);
        }
        return sb.toString();
    }

    static Object summarizeResponseBody(ObjectMapper objectMapper, String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            Map<String, Object> summary = new LinkedHashMap<>();
            if (root.has("success")) {
                summary.put("success", root.get("success").asBoolean());
            }
            if (root.has("message")) {
                summary.put("message", root.get("message").asText());
            }
            JsonNode data = root.get("data");
            if (data != null && !data.isNull()) {
                summary.put("data", summarizeJsonNode(data));
            }
            if (root.has("errorCode")) {
                summary.put("errorCode", root.get("errorCode").asText());
            }
            return summary.isEmpty() ? preview(body) : summary;
        } catch (Exception ignored) {
            return preview(body);
        }
    }

    static String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= PREVIEW_MAX_CHARS) {
            return compact;
        }
        return compact.substring(0, PREVIEW_MAX_CHARS) + "...(" + compact.length() + " chars)";
    }

    private static Object summarizeJsonNode(JsonNode node) {
        if (node.isArray()) {
            return node.size() + " items";
        }
        if (!node.isObject()) {
            return node.asText();
        }
        Map<String, Object> fields = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                fields.put(entry.getKey(), value.size() + " items");
            } else if (value.isObject()) {
                fields.put(entry.getKey(), value.size() + " fields");
            } else if (value.isTextual() && value.asText().length() > 120) {
                fields.put(entry.getKey(), value.asText().substring(0, 120) + "...");
            } else if (value.isValueNode()) {
                fields.put(entry.getKey(), value.isNull() ? null : value.asText());
            } else {
                fields.put(entry.getKey(), value.toString());
            }
        });
        return fields;
    }

    private static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
