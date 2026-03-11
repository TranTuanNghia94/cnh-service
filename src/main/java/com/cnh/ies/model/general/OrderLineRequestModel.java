package com.cnh.ies.model.general;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Data;

@Data
public class OrderLineRequestModel {
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
