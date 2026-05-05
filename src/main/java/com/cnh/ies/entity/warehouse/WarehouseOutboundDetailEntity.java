package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.order.OrderLineEntity;
import com.cnh.ies.entity.product.ProductEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "warehouse_outbound_details")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseOutboundDetailEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_id", nullable = false)
    private WarehouseOutboundEntity outbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLineEntity orderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "box", length = 100)
    private String box;

    @Column(name = "reference_code", length = 200)
    private String referenceCode;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "price_without_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceWithoutTax = BigDecimal.ZERO;

    @Column(name = "vat", nullable = false, precision = 6, scale = 2)
    private BigDecimal vat = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
