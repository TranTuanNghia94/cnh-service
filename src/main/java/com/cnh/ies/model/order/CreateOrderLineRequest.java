package com.cnh.ies.model.order;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Data;

@Data
public class CreateOrderLineRequest {
    private Optional<String> id;
    private Optional<String> orderId;
    private Optional<String> productId;
    private Optional<String> vendorId;
    private String productCodeSuggest;
    private String productNameSuggest;
    private String vendorCodeSuggest;
    private String vendorNameSuggest;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String uom;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private Boolean isIncludedTax;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private Optional<Boolean> isDeleted;
    private Optional<String> receiverNote;
    private Optional<String> deliveryNote;
    private Optional<String> referenceNote;
}
