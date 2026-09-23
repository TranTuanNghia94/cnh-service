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
public class PurchaseOverallReport {

    private ServiceReportDateRangeInfo dateRange;
    private Long purchaseOrderCount;
    private List<StatusBreakdownItem> statusBreakdown;
    private BigDecimal totalPurchasedQuantity;
    private BigDecimal totalBeforeTax;
    private BigDecimal totalPrice;
    private BigDecimal totalPriceVnd;
    private Long distinctVendorCount;
    private Long distinctProductCount;
}
