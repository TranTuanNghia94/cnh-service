package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseInboundFeeRequest {
    private String feeName;
    private String feeType;
    private BigDecimal amount;
    private String note;
}
