package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseStockTransactionInfo {
    private String id;
    private String productId;
    private String direction;
    private BigDecimal quantity;
    private String referenceType;
    private String referenceId;
    private String note;
    private String createdAt;
}
