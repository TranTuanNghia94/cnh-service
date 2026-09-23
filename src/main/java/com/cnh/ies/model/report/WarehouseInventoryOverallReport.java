package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseInventoryOverallReport {

    private ServiceReportDateRangeInfo dateRange;
    private Long productCount;
    private BigDecimal totalQuantityOnHand;
    private BigDecimal inboundMovementQuantity;
    private BigDecimal outboundMovementQuantity;
    private BigDecimal netMovementQuantity;
    private Long movementCount;
}
