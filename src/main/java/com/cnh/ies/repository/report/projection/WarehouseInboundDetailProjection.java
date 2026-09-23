package com.cnh.ies.repository.report.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface WarehouseInboundDetailProjection {

    UUID getReceiptId();

    String getReceiptNumber();

    LocalDate getReceivedDate();

    String getStatus();

    String getPaymentRequestNumber();

    String getVendorCode();

    String getVendorName();

    String getProductCode();

    String getProductName();

    BigDecimal getQuantityExpected();

    BigDecimal getQuantityReceived();

    BigDecimal getFeeAmount();

    BigDecimal getRealBillAmount();

    BigDecimal getBillOnPaperAmount();

    String getCreatedBy();
}
