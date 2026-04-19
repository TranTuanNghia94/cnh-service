package com.cnh.ies.model.purchaseorder;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Data;

@Data
public class CreatePurchaseOrderLineRequest {
    private Optional<String> id;
    private Optional<String> purchaseOrderId;
    private Optional<String> saleOrderLineId;
    private Optional<String> productId;
    private Optional<String> vendorId;
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
    private Optional<Boolean> isDeleted;
}
