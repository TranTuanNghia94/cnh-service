package com.cnh.ies.entity.purchaseorder;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
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
@Table(name = "purchase_order_lines")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseOrderLineEntity extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_orderline_id")
    private OrderLineEntity saleOrderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private VendorsEntity vendor;

    @Column(name = "link", columnDefinition = "TEXT")
    private String link;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "uom_1", length = 50)
    private String uom1;

    @Column(name = "uom_2", length = 50)
    private String uom2;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "is_tax_included")
    private Boolean isTaxIncluded = false;

    @Column(name = "tax", precision = 5, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "total_before_tax", precision = 15, scale = 0)
    private BigDecimal totalBeforeTax;

    @Column(name = "total_price", precision = 15, scale = 0)
    private BigDecimal totalPrice;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "exchange_rate", precision = 15, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "total_price_vnd", precision = 15, scale = 0)
    private BigDecimal totalPriceVnd;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "quote", columnDefinition = "TEXT")
    private String quote;

    @Column(name = "invoice", columnDefinition = "TEXT")
    private String invoice;

    @Column(name = "bill_of_ladding", columnDefinition = "TEXT")
    private String billOfLadding;

    @Column(name = "receipt_warehouse", columnDefinition = "TEXT")
    private String receiptWarehouse;

    @Column(name = "track_id", columnDefinition = "TEXT")
    private String trackId;

    @Column(name = "purchase_contract_number", columnDefinition = "TEXT")
    private String purchaseContractNumber;
}
