package com.cnh.ies.repository.report.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface OrderDetailProjection {

    UUID getOrderId();

    String getOrderNumber();

    LocalDate getOrderDate();

    String getStatus();

    String getCustomerName();

    String getContractNumber();

    String getProductCode();

    String getProductName();

    String getVendorCode();

    BigDecimal getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getDiscountAmount();

    BigDecimal getTaxAmount();

    BigDecimal getTotalAmount();

    String getCreatedBy();
}
