package com.cnh.ies.service.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cnh.ies.exception.ApiException;

class ExportJobTypeTest {

    @Test
    void normalize_acceptsValidTypes() {
        assertEquals(ExportJobType.PRODUCTS, ExportJobType.normalize("products", "rid"));
        assertEquals(ExportJobType.VENDORS, ExportJobType.normalize("VENDORS", "rid"));
        assertEquals(ExportJobType.CUSTOMERS, ExportJobType.normalize("customers", "rid"));
        assertEquals(ExportJobType.WAREHOUSE_INVENTORY,
                ExportJobType.normalize("warehouse_inventory", "rid"));
    }

    @Test
    void normalize_acceptsServiceReportTypes() {
        assertEquals(ExportJobType.SERVICE_ORDER_OVERALL,
                ExportJobType.normalize("service_order_overall", "rid"));
        assertEquals(ExportJobType.SERVICE_PAYMENT_REQUEST_DETAIL,
                ExportJobType.normalize("SERVICE_PAYMENT_REQUEST_DETAIL", "rid"));
    }

    @Test
    void validateServiceReportParams_requiresDates() {
        assertThrows(ApiException.class, () -> ExportJobType.validateServiceReportParams(
                ExportJobType.SERVICE_ORDER_OVERALL, null, null, "rid"));
    }

    @Test
    void validateOperationalReportParams_matchesEachReportFilter() {
        assertEquals(ExportJobType.REPORT_STOCK, ExportJobType.normalize("report_stock", "rid"));
        ExportJobType.validateOperationalReportParams(
                ExportJobType.REPORT_STOCK, null, null, Map.of("month", "9", "year", "2026"), "rid");
        ExportJobType.validateOperationalReportParams(
                ExportJobType.REPORT_INBOUND, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 23), null, "rid");
        ExportJobType.validateOperationalReportParams(
                ExportJobType.REPORT_VENDOR_DEBT, null, null, null, "rid");
        assertThrows(ApiException.class, () -> ExportJobType.validateOperationalReportParams(
                ExportJobType.REPORT_STOCK, null, null, Map.of(), "rid"));
        assertThrows(ApiException.class, () -> ExportJobType.validateOperationalReportParams(
                ExportJobType.REPORT_OUTBOUND_DETAIL, null, null, null, "rid"));
    }

    @Test
    void normalize_rejectsInvalidType() {
        assertThrows(ApiException.class, () -> ExportJobType.normalize("ORDERS", "rid"));
    }

    @Test
    void fileNamePrefix_mapsTypes() {
        assertEquals("products", ExportJobType.fileNamePrefix(ExportJobType.PRODUCTS));
        assertEquals("warehouse-inventory", ExportJobType.fileNamePrefix(ExportJobType.WAREHOUSE_INVENTORY));
    }
}
