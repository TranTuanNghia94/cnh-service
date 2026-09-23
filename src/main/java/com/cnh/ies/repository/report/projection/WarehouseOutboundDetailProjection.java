package com.cnh.ies.repository.report.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface WarehouseOutboundDetailProjection {

    UUID getOutboundId();

    String getOutboundNumber();

    LocalDate getOutboundDate();

    String getStatus();

    String getContractNumber();

    String getOrderNumber();

    String getProductCode();

    String getProductName();

    BigDecimal getQuantity();

    BigDecimal getUnitPrice();

    BigDecimal getTotalAmount();

    BigDecimal getTaxAmount();

    String getCreatedBy();
}
