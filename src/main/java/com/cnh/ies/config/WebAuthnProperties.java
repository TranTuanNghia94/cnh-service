package com.cnh.ies.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "webauthn")
public class WebAuthnProperties {

    private String rpId = "localhost";
    private String rpName = "CNH WMS";
    private String origins = "http://localhost:3000";
    private int challengeTtlSeconds = 300;

    /**
     * WebAuthn rpId must be a registrable domain suffix without scheme or port.
     */
    public String getRpId() {
        if (rpId == null || rpId.isBlank()) {
            return "localhost";
        }
        String normalized = rpId.trim();
        int portIdx = normalized.indexOf(':');
        if (portIdx > 0) {
            normalized = normalized.substring(0, portIdx);
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            normalized = normalized.replaceFirst("^https?://", "");
            int slash = normalized.indexOf('/');
            if (slash >= 0) {
                normalized = normalized.substring(0, slash);
            }
            int p = normalized.indexOf(':');
            if (p > 0) {
                normalized = normalized.substring(0, p);
            }
        }
        return normalized;
    }

    public Set<String> getAllowedOrigins() {
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
