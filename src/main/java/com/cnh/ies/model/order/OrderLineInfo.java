package com.cnh.ies.model.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderLineInfo {
    private String id;
    private String orderId;
    private String productId;
    private String productName;
    private String vendorId;
    private String vendorName;
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
    private String receiverNote;
    private String deliveryNote;
    private String referenceNote;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
}
