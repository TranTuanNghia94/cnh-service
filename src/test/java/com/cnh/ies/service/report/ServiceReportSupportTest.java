package com.cnh.ies.service.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.cnh.ies.exception.ApiException;

class ServiceReportSupportTest {

    @Test
    void validateDateRange_rejectsInvertedRange() {
        assertThrows(ApiException.class, () -> ServiceReportSupport.validateDateRange(
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), "rid"));
    }

    @Test
    void pagedList_calculatesTotalPages() {
        var list = ServiceReportSupport.pagedList(java.util.List.of("a"), 0, 10, 25);
        assertEquals(3, list.getPagination().getTotalPage());
        assertEquals(25L, list.getPagination().getTotal());
    }
}
