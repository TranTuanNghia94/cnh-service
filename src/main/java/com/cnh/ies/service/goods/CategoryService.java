package com.cnh.ies.service.goods;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.goods.CategoryInfo;
import com.cnh.ies.model.goods.CreateCategoryRequest;
import com.cnh.ies.model.goods.UpdateCategoryRequest;
import com.cnh.ies.entity.goods.CategoryEntity;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
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

    public ListDataModel<CategoryInfo> getAllCategories(String requestId) {
        try {
            log.info("Getting all categories with request: {}", requestId);

            List<CategoryEntity> categories = categoryRepo.findAll();
            List<CategoryInfo> categoryInfos = categories.stream().map(categoryMapper::toCategoryInfo).collect(Collectors.toList());


            PaginationModel pagination = PaginationModel.builder()
                .page(1)
                .limit(10)
                .total((long) categories.size())
                .totalPage(1)
                .build();
                
            log.info("Categories fetched successfully with request: {}", requestId);

            return ListDataModel.<CategoryInfo>builder()
                .data(categoryInfos)
                .pagination(pagination)
                .build();
        } catch (Exception e) {
            log.error("Error getting all categories", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all categories", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
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

    @Transactional
    public String updateCategory(UpdateCategoryRequest request, String requestId) {
        try {
            log.info("Updating category with request: {}", request);

            Optional<CategoryEntity> category = categoryRepo.findById(UUID.fromString(request.getId()));

            if (category.isEmpty()) {
                log.error("Category not found with id: {}", request.getId());
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found with id: " + request.getId(), HttpStatus.NOT_FOUND.value(), requestId);
            }

            category.get().setName(request.getName());
            category.get().setCode(request.getCode());
            category.get().setUnit(request.getUnit());
            category.get().setDescription(request.getDescription());
            category.get().setUpdatedAt(Instant.now());

            categoryRepo.save(category.get());

            log.info("Category updated successfully with request: {}", request);
        
            return "Category updated successfully";
        } catch (Exception e) {
            log.error("Error updating category", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error updating category", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public String deleteCategory(String id, String requestId) {
        try {
            log.info("Deleting category with id: {}", id);

            Optional<CategoryEntity> category = categoryRepo.findById(UUID.fromString(id));

            if (category.isEmpty()) {
                log.error("Category not found with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found with id: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            category.get().setCode(category.get().getCode() + "_" + "DELETED" + "_" + requestId);
            category.get().setIsDeleted(true);
            category.get().setUpdatedAt(Instant.now());
            categoryRepo.save(category.get());

            log.info("Category deleted successfully with id: {}", id);

            return "Category deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting category", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting category", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    @Transactional
    public String restoreCategory(String id, String requestId) {
        try {
            log.info("Restoring category with id: {}", id);

        Optional<CategoryEntity> category = categoryRepo.findById(UUID.fromString(id));

        if (category.isEmpty()) {
            log.error("Category not found with id: {}", id);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found with id: " + id, HttpStatus.NOT_FOUND.value(), requestId);
        }
        
        category.get().setCode(category.get().getCode().replace("_DELETED_" + requestId, ""));
        category.get().setIsDeleted(false);
        category.get().setUpdatedAt(Instant.now());
        categoryRepo.save(category.get());

        log.info("Category restored successfully with id: {}", id);

        return "Category restored successfully";
        
        } catch (Exception e) {
            log.error("Error restoring category", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error restoring category", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public CategoryInfo getCategoryById(String id, String requestId) {
        try {
            log.info("Getting category by id: {}", id);

            Optional<CategoryEntity> category = categoryRepo.findById(UUID.fromString(id));

            if (category.isEmpty()) {
                log.error("Category not found with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found with id: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (category.get().getIsDeleted()) {
                log.error("Category is deleted with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category is deleted with id: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Category fetched successfully with id: {}", id);

        return categoryMapper.toCategoryInfo(category.get());
        } catch (Exception e) {
            log.error("Error getting category by id", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting category by id", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
