package com.cnh.ies.entity.order;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.entity.vendors.VendorsEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "order_lines")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderLineEntity extends BaseEntity {
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorsEntity vendor;

    @Column(name = "product_code_suggest", length = 200)
    private String productCodeSuggest;

    @Column(name = "product_name_suggest", length = 200)
    private String productNameSuggest;

    @Column(name = "vendor_code_suggest", length = 200)
    private String vendorCodeSuggest;

    @Column(name = "vendor_name_suggest", length = 200)
    private String vendorNameSuggest;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "uom", nullable = false, length = 50)
    private String uom;
    
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountAmount;
    
    @Column(name = "is_included_tax", nullable = false)
    private Boolean isIncludedTax;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;
    
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "receiver_note", columnDefinition = "TEXT")
    private String receiverNote;
    
    @Column(name = "delivery_note", columnDefinition = "TEXT")
    private String deliveryNote;
    
    @Column(name = "reference_note", columnDefinition = "TEXT")
    private String referenceNote;

}
