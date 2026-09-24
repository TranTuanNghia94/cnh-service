package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesDetailReportRow {
    private String csName;
    private String customerCode;
    private String contractNumber;
    private String contractDate;
    private String productCode;
    private String productName;
    private String unit;
    private BigDecimal salesQuantity;
    private BigDecimal salesUnitPrice;
    private BigDecimal salesAmount;
    private String taxRate;
    private String suggestedVendorCode;
    private String reference;
    private String teacher;
    private String department;
    private String note;
    private String confirmedVendorCode;
    private String paymentPaperType;
    private String paymentPaperNumber;
    private BigDecimal purchaseQuantity;
    private String currency;
    private BigDecimal purchaseUnitPrice;
    private BigDecimal purchaseAmount;
    private String quoteNumber;
    private String invoiceNumber;
    private String billNumber;
    private String receiptWarehouse;
    private String trackingNumber;
    private String inboundDate;
    private String inboundNumber;
    private BigDecimal inboundQuantity;
    private BigDecimal inboundUnitPrice;
    private BigDecimal inboundAmount;
    private String inboundInvoiceNumber;
    private String bookBillNumber;
    private String inboundNote;
}
