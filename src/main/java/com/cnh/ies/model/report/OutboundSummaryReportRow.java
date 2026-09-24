package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboundSummaryReportRow {
    private String userName;
    private String outboundDate;
    private String outboundNumber;
    private String contractNumber;
    private String customerName;
    private BigDecimal amountBeforeTax;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
}
