package com.cnh.ies.model.report;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDetailRow {

    private String paymentRequestId;
    private String requestNumber;
    private Instant requestDate;
    private String status;
    private String vendorCode;
    private String vendorName;
    private String productCode;
    private String purchaseOrderNumber;
    private BigDecimal requestedAmount;
    private BigDecimal paidAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private Integer currentApprovalLevel;
    private Integer approvalLevels;
    private String createdBy;
}
