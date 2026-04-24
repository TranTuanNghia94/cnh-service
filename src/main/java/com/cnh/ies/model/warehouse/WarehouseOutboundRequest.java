package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseOutboundRequest {
    private String productId;
    private BigDecimal quantity;
    private String referenceType;
    private String referenceId;
    private String note;
}
