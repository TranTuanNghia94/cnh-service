package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.goods.CategoryInfo;
import com.cnh.ies.model.goods.CreateCategoryRequest;
import com.cnh.ies.model.goods.UpdateCategoryRequest;
import com.cnh.ies.model.general.GeneralRequest;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.service.goods.CategoryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ApiResponse<ListDataModel<CategoryInfo>> getAllCategories() {

        String requestId = UUID.randomUUID().toString();
        log.info("Getting all categories with initiated requestId: {}", requestId);

        ListDataModel<CategoryInfo> response = categoryService.getAllCategories(requestId);

        log.info("Getting all categories success with requestId: {}", requestId);

        return ApiResponse.success(response, "Get all categories success");
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryInfo> createCategory(@RequestBody CreateCategoryRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating category with initiated requestId: {}", requestId);

        CategoryInfo response = categoryService.createCategory(request, requestId);

        log.info("Creating category success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create category success");
    }

    @PutMapping("/categories")
    public ApiResponse<String> updateCategory(@RequestBody UpdateCategoryRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating category with initiated requestId: {}", requestId);

        String response = categoryService.updateCategory(request, requestId);

        log.info("Updating category success with requestId: {}", requestId);

        return ApiResponse.success(response, "Update category success");
    }

    @DeleteMapping("/categories")
    public ApiResponse<String> deleteCategory(@RequestBody GeneralRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting category with initiated requestId: {}", requestId);

        String response = categoryService.deleteCategory(request.getRequestId(), requestId);

        log.info("Deleting category success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete category success");
    }
}
