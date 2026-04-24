package com.cnh.ies.entity.warehouse;

import java.math.BigDecimal;

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
@Table(name = "warehouse_inventory")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseInventoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "quantity_on_hand", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;
}
