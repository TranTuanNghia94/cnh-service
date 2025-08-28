package com.cnh.ies.model.goods;

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
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
