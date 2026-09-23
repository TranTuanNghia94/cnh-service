package com.cnh.ies.model.report;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseInboundOverallReport {

    private ServiceReportDateRangeInfo dateRange;
    private Long receiptCount;
    private List<StatusBreakdownItem> statusBreakdown;
    private BigDecimal totalFeeAmount;
    private BigDecimal totalRealBillAmount;
    private BigDecimal totalBillOnPaperAmount;
    private BigDecimal totalExpectedQuantity;
    private BigDecimal totalReceivedQuantity;
}
