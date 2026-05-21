package com.cnh.ies.service.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void normalize_rejectsInvalidType() {
        assertThrows(ApiException.class, () -> ExportJobType.normalize("ORDERS", "rid"));
    }

    @Test
    void fileNamePrefix_mapsTypes() {
        assertEquals("products", ExportJobType.fileNamePrefix(ExportJobType.PRODUCTS));
        assertEquals("warehouse-inventory", ExportJobType.fileNamePrefix(ExportJobType.WAREHOUSE_INVENTORY));
    }
}
