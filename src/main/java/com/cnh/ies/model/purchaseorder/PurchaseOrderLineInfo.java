package com.cnh.ies.model.purchaseorder;

import java.math.BigDecimal;

import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.model.vendors.VendorInfo;

import lombok.Data;

@Data
public class PurchaseOrderLineInfo {
    private String id;
    private String purchaseOrderId;
    private String saleOrderLineId;
    private String productId;
    private String productName;
    private String vendorId;
    private String vendorName;
    private ProductInfo product;
    private VendorInfo vendor;
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
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
    private String createdBy;
    private String updatedBy;
    private BigDecimal processPercentage;
    private BigDecimal purchaseOrderQuantity;
    private BigDecimal orderDetailQuantity;
    private String processQuantityDetail;
}
