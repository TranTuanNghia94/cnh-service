package com.cnh.ies.service.export;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.export.ServiceReportExportParams;

public final class ExportJobType {

    public static final String PRODUCTS = "PRODUCTS";
    public static final String VENDORS = "VENDORS";
    public static final String CUSTOMERS = "CUSTOMERS";
    public static final String WAREHOUSE_INVENTORY = "WAREHOUSE_INVENTORY";

    public static final String SERVICE_WAREHOUSE_INBOUND_OVERALL = "SERVICE_WAREHOUSE_INBOUND_OVERALL";
    public static final String SERVICE_WAREHOUSE_INBOUND_DETAIL = "SERVICE_WAREHOUSE_INBOUND_DETAIL";
    public static final String SERVICE_WAREHOUSE_OUTBOUND_OVERALL = "SERVICE_WAREHOUSE_OUTBOUND_OVERALL";
    public static final String SERVICE_WAREHOUSE_OUTBOUND_DETAIL = "SERVICE_WAREHOUSE_OUTBOUND_DETAIL";
    public static final String SERVICE_WAREHOUSE_INVENTORY_OVERALL = "SERVICE_WAREHOUSE_INVENTORY_OVERALL";
    public static final String SERVICE_WAREHOUSE_INVENTORY_DETAIL = "SERVICE_WAREHOUSE_INVENTORY_DETAIL";
    public static final String SERVICE_ORDER_OVERALL = "SERVICE_ORDER_OVERALL";
    public static final String SERVICE_ORDER_DETAIL = "SERVICE_ORDER_DETAIL";
    public static final String SERVICE_PURCHASE_OVERALL = "SERVICE_PURCHASE_OVERALL";
    public static final String SERVICE_PAYMENT_REQUEST_OVERALL = "SERVICE_PAYMENT_REQUEST_OVERALL";
    public static final String SERVICE_PAYMENT_REQUEST_DETAIL = "SERVICE_PAYMENT_REQUEST_DETAIL";

    private static final Set<String> LEGACY_TYPES = Set.of(
            PRODUCTS, VENDORS, CUSTOMERS, WAREHOUSE_INVENTORY);

    private static final Set<String> SERVICE_REPORT_TYPES = Set.of(
            SERVICE_WAREHOUSE_INBOUND_OVERALL,
            SERVICE_WAREHOUSE_INBOUND_DETAIL,
            SERVICE_WAREHOUSE_OUTBOUND_OVERALL,
            SERVICE_WAREHOUSE_OUTBOUND_DETAIL,
            SERVICE_WAREHOUSE_INVENTORY_OVERALL,
            SERVICE_WAREHOUSE_INVENTORY_DETAIL,
            SERVICE_ORDER_OVERALL,
            SERVICE_ORDER_DETAIL,
            SERVICE_PURCHASE_OVERALL,
            SERVICE_PAYMENT_REQUEST_OVERALL,
            SERVICE_PAYMENT_REQUEST_DETAIL);

    private ExportJobType() {}

    public static String normalize(String raw, String requestId) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Export type is required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (!LEGACY_TYPES.contains(normalized) && !SERVICE_REPORT_TYPES.contains(normalized)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "Invalid export type: " + raw,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return normalized;
    }

    public static boolean isServiceReportType(String type) {
        return SERVICE_REPORT_TYPES.contains(type);
    }

    public static void validateServiceReportParams(
            String type, LocalDate fromDate, LocalDate toDate, String requestId) {
        if (!isServiceReportType(type)) {
            return;
        }
        if (fromDate == null || toDate == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "fromDate and toDate are required for service report export type: " + type,
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST,
                    "fromDate must be on or before toDate",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
    }

    public static String fileNamePrefix(String type) {
        return switch (type) {
            case PRODUCTS -> "products";
            case VENDORS -> "vendors";
            case CUSTOMERS -> "customers";
            case WAREHOUSE_INVENTORY -> "warehouse-inventory";
            case SERVICE_WAREHOUSE_INBOUND_OVERALL -> "service-warehouse-inbound-overall";
            case SERVICE_WAREHOUSE_INBOUND_DETAIL -> "service-warehouse-inbound-detail";
            case SERVICE_WAREHOUSE_OUTBOUND_OVERALL -> "service-warehouse-outbound-overall";
            case SERVICE_WAREHOUSE_OUTBOUND_DETAIL -> "service-warehouse-outbound-detail";
            case SERVICE_WAREHOUSE_INVENTORY_OVERALL -> "service-warehouse-inventory-overall";
            case SERVICE_WAREHOUSE_INVENTORY_DETAIL -> "service-warehouse-inventory-detail";
            case SERVICE_ORDER_OVERALL -> "service-order-overall";
            case SERVICE_ORDER_DETAIL -> "service-order-detail";
            case SERVICE_PURCHASE_OVERALL -> "service-purchase-overall";
            case SERVICE_PAYMENT_REQUEST_OVERALL -> "service-payment-request-overall";
            case SERVICE_PAYMENT_REQUEST_DETAIL -> "service-payment-request-detail";
            default -> "export";
        };
    }

    public static String displayTypeName(String type) {
        return switch (type) {
            case PRODUCTS -> "sản phẩm";
            case VENDORS -> "nhà cung cấp";
            case CUSTOMERS -> "khách hàng";
            case WAREHOUSE_INVENTORY -> "tồn kho";
            case SERVICE_WAREHOUSE_INBOUND_OVERALL -> "báo cáo nhập kho tổng hợp";
            case SERVICE_WAREHOUSE_INBOUND_DETAIL -> "báo cáo nhập kho chi tiết";
            case SERVICE_WAREHOUSE_OUTBOUND_OVERALL -> "báo cáo xuất kho tổng hợp";
            case SERVICE_WAREHOUSE_OUTBOUND_DETAIL -> "báo cáo xuất kho chi tiết";
            case SERVICE_WAREHOUSE_INVENTORY_OVERALL -> "báo cáo tồn kho tổng hợp";
            case SERVICE_WAREHOUSE_INVENTORY_DETAIL -> "báo cáo tồn kho chi tiết";
            case SERVICE_ORDER_OVERALL -> "báo cáo đơn hàng tổng hợp";
            case SERVICE_ORDER_DETAIL -> "báo cáo đơn hàng chi tiết";
            case SERVICE_PURCHASE_OVERALL -> "báo cáo mua hàng tổng hợp";
            case SERVICE_PAYMENT_REQUEST_OVERALL -> "báo cáo đề nghị thanh toán tổng hợp";
            case SERVICE_PAYMENT_REQUEST_DETAIL -> "báo cáo đề nghị thanh toán chi tiết";
            default -> type;
        };
    }
}
