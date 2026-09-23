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
public class WarehouseOutboundOverallReport {

    private ServiceReportDateRangeInfo dateRange;
    private Long outboundCount;
    private List<StatusBreakdownItem> statusBreakdown;
    private BigDecimal totalAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalQuantity;
}
