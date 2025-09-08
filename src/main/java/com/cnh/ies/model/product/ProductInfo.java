package com.cnh.ies.model.product;

import lombok.Data;

@Data
public class ProductInfo {
    private String id;
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
    private String categoryName;
    private String categoryId;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
