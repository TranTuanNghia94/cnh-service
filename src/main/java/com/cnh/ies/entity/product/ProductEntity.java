package com.cnh.ies.entity.product;

import jakarta.persistence.*;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.cnh.ies.entity.BaseEntity;

@Entity
@Table(name = "products")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductEntity extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "barcode", length = 100)
    private String barcode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "unit_1", length = 20)
    private String unit1;

    @Column(name = "unit_2", length = 20)
    private String unit2;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "tax", precision = 15, scale = 2, nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "misa_code", length = 100)
    private String misaCode;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
