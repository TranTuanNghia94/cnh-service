package com.cnh.ies.model.purchaseorder;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UpdatePurchaseOrderLineRequest {
    private String id;
    private String productId;
    private String vendorId;
    private String saleOrderLineId;
    private String link;
    private BigDecimal quantity;
    private String uom1;
    private String uom2;
    private BigDecimal unitPrice;
    private Boolean isTaxIncluded;
    private BigDecimal tax;
    private BigDecimal totalBeforeTax;
    private BigDecimal totalPrice;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal totalPriceVnd;
    private String note;
    private String quote;
    private String invoice;
    private String billOfLadding;
    private String receiptWarehouse;
    private String trackId;
    private String purchaseContractNumber;
}
