package com.cnh.ies.model.report;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockLedgerRow {
    private String productId;
    private String categoryName;
    private String productCode;
    private String productName;
    private String unit;
    private BigDecimal beginningQuantity;
    private BigDecimal inboundQuantity;
    private BigDecimal outboundQuantity;
    private BigDecimal endingQuantity;
    private BigDecimal unitPrice;
}
