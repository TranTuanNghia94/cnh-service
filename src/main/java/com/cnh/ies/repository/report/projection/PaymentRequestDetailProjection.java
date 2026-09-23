package com.cnh.ies.repository.report.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PaymentRequestDetailProjection {

    UUID getPaymentRequestId();

    String getRequestNumber();

    Instant getRequestDate();

    String getStatus();

    String getVendorCode();

    String getVendorName();

    String getProductCode();

    String getPurchaseOrderNumber();

    BigDecimal getRequestedAmount();

    BigDecimal getPaidAmount();

    BigDecimal getFeeAmount();

    BigDecimal getTotalAmount();

    Integer getCurrentApprovalLevel();

    Integer getApprovalLevels();

    String getCreatedBy();
}
