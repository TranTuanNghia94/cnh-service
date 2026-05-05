package com.cnh.ies.entity.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "product_tax_histories")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductTaxHistoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "old_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal oldTax;

    @Column(name = "new_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal newTax;

    @Column(name = "source_type", nullable = false, length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
