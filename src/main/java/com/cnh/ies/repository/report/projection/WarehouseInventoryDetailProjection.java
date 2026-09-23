package com.cnh.ies.repository.report.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface WarehouseInventoryDetailProjection {

    UUID getTransactionId();

    String getProductCode();

    String getProductName();

    String getDirection();

    BigDecimal getQuantity();

    String getReferenceType();

    UUID getReferenceId();

    Instant getCreatedAt();

    String getNote();

    String getCreatedBy();
}
