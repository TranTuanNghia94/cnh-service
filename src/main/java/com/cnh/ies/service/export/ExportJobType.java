package com.cnh.ies.service.export;

import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.cnh.ies.exception.ApiException;

public final class ExportJobType {

    public static final String PRODUCTS = "PRODUCTS";
    public static final String VENDORS = "VENDORS";
    public static final String CUSTOMERS = "CUSTOMERS";
    public static final String WAREHOUSE_INVENTORY = "WAREHOUSE_INVENTORY";

    private ExportJobType() {}

    public static String normalize(String raw, String requestId) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Export type is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!PRODUCTS.equals(normalized) && !VENDORS.equals(normalized) && !CUSTOMERS.equals(normalized)
                && !WAREHOUSE_INVENTORY.equals(normalized)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Invalid export type. Allowed: PRODUCTS, VENDORS, CUSTOMERS, WAREHOUSE_INVENTORY",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return normalized;
    }

    public static String fileNamePrefix(String type) {
        return switch (type) {
            case PRODUCTS -> "products";
            case VENDORS -> "vendors";
            case CUSTOMERS -> "customers";
            case WAREHOUSE_INVENTORY -> "warehouse-inventory";
            default -> "export";
        };
    }
}
