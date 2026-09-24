package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboundDetailReportRow {
    private String postingDate;
    private String documentDate;
    private String documentNumber;
    private String outboundNumber;
    private String reason;
    private String invoiceNumber;
    private String invoiceDate;
    private String customerCode;
    private String customerName;
    private String narrative;
    private String productCode;
    private String productName;
    private String debitAccount;
    private String creditAccount;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private String vatAccount;
    private String warehouseCode;
    private String cogsAccount;
    private String inventoryAccount;
    private String businessUnit;
}
