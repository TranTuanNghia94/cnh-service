package com.cnh.ies.mapper.goods;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.goods.CategoryEntity;
import com.cnh.ies.model.goods.CategoryInfo;
import com.cnh.ies.model.goods.CreateCategoryRequest;

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
}
