package com.cnh.ies.model.product;

import java.util.Optional;

import lombok.Data;

@Data
public class CreateProductRequest {
    private String name;
    private String code;
    private String unit1;
    private Optional<String> unit2;
    private Optional<Float> price;
    private Optional<Float> tax;
    private Optional<String> misaCode;
    private Optional<Float> costPrice;
    private Optional<String> imageUrl;
    private Optional<String> description;
    private String categoryId;
    private Boolean isActive;
}
