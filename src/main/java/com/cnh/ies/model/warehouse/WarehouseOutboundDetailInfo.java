package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseOutboundDetailInfo {
    private String id;
    private String orderLineId;
    private String productId;
    private String productCode;
    private String productName;
    private BigDecimal quantity;
    private String box;
    private String referenceCode;
    private BigDecimal unitPrice;
    private BigDecimal priceWithoutTax;
    private BigDecimal vat;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String note;
}
