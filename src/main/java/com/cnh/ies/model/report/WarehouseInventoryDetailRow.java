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
public class WarehouseInventoryDetailRow {

    private String transactionId;
    private String productCode;
    private String productName;
    private String direction;
    private BigDecimal quantity;
    private String referenceType;
    private String referenceId;
    private Instant createdAt;
    private String note;
    private String createdBy;
}
