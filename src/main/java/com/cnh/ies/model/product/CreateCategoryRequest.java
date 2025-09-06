package com.cnh.ies.model.product;

import java.util.Optional;

import lombok.Data;

@Data
public class CreateCategoryRequest {
    private String name;
    private String code;
    private String unit;
    private String description;
    private Optional<String> parentId;
}
