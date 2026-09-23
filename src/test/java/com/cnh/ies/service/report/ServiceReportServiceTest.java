package com.cnh.ies.service.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cnh.ies.model.report.OrderReportRequest;
import com.cnh.ies.model.report.WarehouseInboundReportRequest;
import com.cnh.ies.repository.report.ServiceReportRepository;
import com.cnh.ies.repository.report.ServiceReportRepository.StockMovementTotals;
import com.cnh.ies.repository.report.projection.StatusCountProjection;

@ExtendWith(MockitoExtension.class)
class ServiceReportServiceTest {

    @Mock
    private ServiceReportRepository reportRepository;

    @InjectMocks
    private ServiceReportService serviceReportService;

    @Test
    void warehouseInboundOverall_aggregatesRepositoryResults() {
        WarehouseInboundReportRequest request = new WarehouseInboundReportRequest();
        request.setFromDate(LocalDate.of(2026, 5, 1));
        request.setToDate(LocalDate.of(2026, 5, 31));

        when(reportRepository.countInboundReceipts(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(3L);
        when(reportRepository.inboundStatusBreakdown(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(statusRow("APPROVED", 2L)));
        when(reportRepository.inboundHeaderMoneyTotals(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal[] {
                        new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")
                });
        when(reportRepository.inboundLineQuantities(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal[] { new BigDecimal("100"), new BigDecimal("90") });

        var report = serviceReportService.warehouseInboundOverall(request, "rid");

        assertEquals(3L, report.getReceiptCount());
        assertEquals(new BigDecimal("10"), report.getTotalFeeAmount());
        assertEquals(new BigDecimal("100"), report.getTotalExpectedQuantity());
        assertEquals(1, report.getStatusBreakdown().size());
        assertEquals("APPROVED", report.getStatusBreakdown().get(0).getStatus());
    }

    @Test
    void warehouseInventoryOverall_computesNetMovement() {
        var request = new com.cnh.ies.model.report.WarehouseInventoryReportRequest();
        request.setFromDate(LocalDate.of(2026, 5, 1));
        request.setToDate(LocalDate.of(2026, 5, 31));

        when(reportRepository.countInventoryProducts(any(), any(), any())).thenReturn(5L);
        when(reportRepository.sumQuantityOnHand(any(), any(), any())).thenReturn(new BigDecimal("200"));
        when(reportRepository.stockMovementTotals(any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovementTotals(new BigDecimal("50"), new BigDecimal("20"), 12L));

        var report = serviceReportService.warehouseInventoryOverall(request, "rid");

        assertEquals(new BigDecimal("30"), report.getNetMovementQuantity());
        assertEquals(12L, report.getMovementCount());
        assertNotNull(report.getDateRange());
    }

    private static StatusCountProjection statusRow(String status, Long count) {
        return new StatusCountProjection() {
            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
