package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InboundDetailReportRow {
    private String documentDate;
    private String receiptNumber;
    private String invoiceNumber;
    private String vendorCode;
    private String productCode;
    private String productName;
    private String warehouseCode;
    private String inventoryAccount;
    private String payableAccount;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal amount;
    private BigDecimal convertedAmount;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal convertedVatAmount;
    private String inputVatAccount;
    private String vatAccount;
    private String billOnPaper;
    private String lineNote;
}
