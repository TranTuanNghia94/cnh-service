package com.cnh.ies.model.product;

import lombok.Data;

@Data
public class CreateProductRequest {
    private String name;
    private String code;
    private String unit1;
    private String unit2;
    private String price;
    private String tax;
    private String misaCode;
    private String costPrice;
    private String imageUrl;
    private String description;
    private String categoryId;
    private Boolean isActive;
}
