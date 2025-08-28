package com.cnh.ies.service.goods;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.goods.CategoryInfo;
import com.cnh.ies.model.goods.CreateCategoryRequest;
import com.cnh.ies.entity.goods.CategoryEntity;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.mapper.goods.CategoryMapper;
import com.cnh.ies.repository.goods.CategoryRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;

    // public ListDataModel<CategoryInfo> getAllCategories(String requestId) {
    //     try {
    //         log.info("Getting all categories with request: {}", requestId);

    //         List<CategoryEntity> categories = categoryRepo.findAll();
    //         List<CategoryInfo> categoryInfos = categories.stream()
    //             .map(categoryMapper::toCategoryInfo)
    //             .collect(Collectors.toList());

    //         log.info("Categories fetched successfully with request: {}", requestId);
           
    //         return new ListDataModel<>(categoryInfos, categories.size(), 1, 10);
    //     } catch (Exception e) {
    //         log.error("Error getting all categories", e);
    //         throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all categories", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    //     }
    // }

    public CategoryInfo createCategory(CreateCategoryRequest request, String requestId) {
        try {
            log.info("Creating category with request: {}", request);

            CategoryEntity category = categoryMapper.toCategoryEntity(request);

            categoryRepo.save(category);

            log.info("Category created successfully with request: {}", request);

            return categoryMapper.toCategoryInfo(category);
        } catch (Exception e) {
            log.error("Error creating category", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating category", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
