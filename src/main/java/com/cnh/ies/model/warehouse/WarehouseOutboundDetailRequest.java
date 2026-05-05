package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseOutboundDetailRequest {
    private String orderLineId;
    private BigDecimal quantity;
    private String box;
    private String referenceCode;
    private String currency;
    private String note;
}
