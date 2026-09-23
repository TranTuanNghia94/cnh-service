package com.cnh.ies.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseInboundDetailRow {

    private String receiptId;
    private String receiptNumber;
    private LocalDate receivedDate;
    private String status;
    private String paymentRequestNumber;
    private String vendorCode;
    private String vendorName;
    private String productCode;
    private String productName;
    private BigDecimal quantityExpected;
    private BigDecimal quantityReceived;
    private BigDecimal feeAmount;
    private BigDecimal realBillAmount;
    private BigDecimal billOnPaperAmount;
    private String createdBy;
}
