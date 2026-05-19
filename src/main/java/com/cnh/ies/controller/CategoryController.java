package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;     
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.product.CategoryInfo;
import com.cnh.ies.model.product.CreateCategoryRequest;
import com.cnh.ies.model.product.UpdateCategoryRequest;
import com.cnh.ies.service.product.CategoryService;

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

    @GetMapping("/list")
    public ApiResponse<ListDataModel<CategoryInfo>> getAllCategories() {

        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<CategoryInfo> response = categoryService.getAllCategories(requestId);
return ApiResponse.success(response, "Get all categories success");
    }

    @PostMapping("/create")
    public ApiResponse<CategoryInfo> createCategory(@RequestBody CreateCategoryRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
CategoryInfo response = categoryService.createCategory(request, requestId);
return ApiResponse.success(response, "Create category success");
    }

    @PutMapping("/update")
    public ApiResponse<String> updateCategory(@RequestBody UpdateCategoryRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = categoryService.updateCategory(request, requestId);
return ApiResponse.success(response, "Update category success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteCategory(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = categoryService.deleteCategory(id, requestId);
return ApiResponse.success(response, "Delete category success");
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryInfo> getCategoryById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
CategoryInfo response = categoryService.getCategoryById(id, requestId);
return ApiResponse.success(response, "Get category by id success");
    }


    @PostMapping("/restore/{id}")
    public ApiResponse<String> restoreCategory(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = categoryService.restoreCategory(id, requestId);
return ApiResponse.success(response, "Restore category success");
    }

}
