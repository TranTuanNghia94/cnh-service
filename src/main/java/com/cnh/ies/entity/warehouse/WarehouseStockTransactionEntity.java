package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;
import java.util.UUID;

import com.cnh.ies.entity.BaseEntity;
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
@Table(name = "warehouse_stock_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseStockTransactionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "direction", nullable = false, length = 20)
    private String direction;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reference_type", nullable = false, length = 80)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
