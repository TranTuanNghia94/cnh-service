package com.cnh.ies.entity.product;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.cnh.ies.entity.BaseEntity;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "unit", length = 100)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
