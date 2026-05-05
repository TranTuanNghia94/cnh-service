package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseOutboundOrderLineInfo {
    private String orderLineId;
    private String productId;
    private String productCode;
    private String productName;
    private BigDecimal orderQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal unitPrice;
    private BigDecimal vat;
    private Boolean includedTax;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
}
