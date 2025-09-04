package com.cnh.ies.mapper.product;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.model.product.CategoryInfo;
import com.cnh.ies.model.product.CreateCategoryRequest;
import com.cnh.ies.model.product.UpdateCategoryRequest;

@Component
public class CategoryMapper {

    public CategoryInfo toCategoryInfo(CategoryEntity category) {
        CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.setId(category.getId().toString());
        categoryInfo.setName(category.getName());
        categoryInfo.setCode(category.getCode());
        categoryInfo.setUnit(category.getUnit());
        categoryInfo.setDescription(category.getDescription());
        categoryInfo.setParentId(category.getParent().getId().toString());
        categoryInfo.setIsActive(category.getIsActive());
        return categoryInfo;
    }

    public CategoryEntity toCategoryEntity(CreateCategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setUnit(request.getUnit());
        category.setDescription(request.getDescription());
        category.setIsActive(true);
        return category;
    }

    public CategoryEntity toCategoryEntity(UpdateCategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.fromString(request.getId()));
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setUnit(request.getUnit());
        category.setDescription(request.getDescription());
        return category;
    }
}
