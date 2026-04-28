package com.cnh.ies.model.warehouse;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WarehouseInboundLinePatchRequest {
    private BigDecimal quantityReceived;
    private BigDecimal taxPercent;
    private Boolean taxIncluded;
    private String billOnPaper;
    private String lineNote;
}
