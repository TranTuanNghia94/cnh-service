package com.cnh.ies.model.goods;

import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String id;
    private String name;
    private String code;
    private String unit;
    private String description;
    private String parentId;
}
