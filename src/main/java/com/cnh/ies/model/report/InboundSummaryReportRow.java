package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InboundSummaryReportRow {
    private String userName;
    private String receivedDate;
    private String receiptNumber;
    private String contractNumber;
    private String vendorName;
    private String currency;
    private BigDecimal foreignValue;
    private BigDecimal vatAmount;
    private BigDecimal feeAmount;
    private BigDecimal foreignTotal;
    private BigDecimal vndTotal;
}
