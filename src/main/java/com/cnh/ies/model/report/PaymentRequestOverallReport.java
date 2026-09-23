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
public class PaymentRequestOverallReport {

    private ServiceReportDateRangeInfo dateRange;
    private Long requestCount;
    private List<StatusBreakdownItem> statusBreakdown;
    private BigDecimal totalRequestedAmount;
    private BigDecimal totalRequestedAmountVnd;
    private BigDecimal totalFeeAmount;
    private BigDecimal totalFeeAmountVnd;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountVnd;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalPaidAmountVnd;
    private BigDecimal totalUnpaidAmount;
    private BigDecimal totalUnpaidAmountVnd;
}
