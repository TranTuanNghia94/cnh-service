package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseInventoryBalanceInfo {
    private String productId;
    private String productCode;
    private String productName;
    private BigDecimal quantityOnHand;
}
