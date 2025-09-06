package com.cnh.ies.model.product;

import java.time.Instant;

import lombok.Data;

@Data
public class CategoryInfo {
    private String id;
    private String name;
    private String code;
    private String unit;
    private String description;
    private String parentId;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
