package com.cnh.ies.service.product;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.mapper.product.ProductMapper;
import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.model.product.UpdateProductRequest;
import com.cnh.ies.repository.product.CategoryRepo;
import com.cnh.ies.repository.product.ProductRepo;
import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.entity.product.CategoryEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final CategoryRepo categoryRepo;
    private final ProductMapper productMapper;
    private final ProductRepo productRepo;

    public ListDataModel<ProductInfo> getAllProducts(String requestId, Integer page, Integer limit) {
        log.info("Getting all products with request: {} page: {} limit: {}", requestId, page, limit);

        Page<ProductEntity> products = productRepo.findAllAndIsDeletedFalse(PageRequest.of(page , limit));
        List<ProductInfo> productInfos = products.stream().map(productMapper::toProductInfo).collect(Collectors.toList());

        PaginationModel pagination = PaginationModel.builder()
            .page(page)
            .limit(limit)
            .total(products.getTotalElements())
            .totalPage(products.getTotalPages())
            .build();

        log.info("Products fetched successfully with request: {} page: {} limit: {}", requestId, page, limit);

        return ListDataModel.<ProductInfo>builder()
            .data(productInfos)
            .pagination(pagination)
            .build();
    }

    public ProductInfo createProduct(CreateProductRequest request, String requestId) {
        log.info("Creating product: {} | RequestId: {}", request, requestId);

        CategoryEntity category = categoryRepo.findById(UUID.fromString(request.getCategoryId()))
            .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found", HttpStatus.NOT_FOUND.value(), requestId));

        if (productRepo.findByCode(request.getCode()).isPresent()) {
            log.error("Product code already exists with code: {} RequestId: {}", request.getCode(), requestId);
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Product code already exists", HttpStatus.BAD_REQUEST.value(), requestId);
        }

        ProductEntity product = productMapper.toProductEntity(request, category);

        productRepo.save(product);

        log.info("Product created successfully with request: {}", requestId);

        return productMapper.toProductInfo(product);
    }

    public ProductInfo getProductById(String id, String requestId) {
        log.info("Getting product by id: {}", id);

        Optional<ProductEntity> product = productRepo.findById(UUID.fromString(id));
        if (product.isEmpty()) {
            log.error("Product not found with id: {}", id);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found", HttpStatus.NOT_FOUND.value(), requestId);
        }

        if (product.get().getIsDeleted()) {
            log.error("Product already deleted with id: {}", id);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product already deleted", HttpStatus.NOT_FOUND.value(), requestId);
        }

        log.info("Product fetched successfully with id: {}", id);

        return productMapper.toProductInfo(product.get());
    }

    public ProductInfo getProductByCode(String code, String requestId) {
        log.info("Getting product by code: {}", code);

        Optional<ProductEntity> product = productRepo.findByCode(code);
        if (product.isEmpty()) {
            log.error("Product not found with code: {}", code);
            throw new ApiException(ApiException.ErrorCode.NO_CONTENT, "Product not found", HttpStatus.NO_CONTENT.value(), requestId);
        }

        if (product.get().getIsDeleted()) {
            log.error("Product already deleted with code: {}", code);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product already deleted", HttpStatus.NOT_FOUND.value(), requestId);
        }

        log.info("Product fetched successfully with code: {}", code);

        return productMapper.toProductInfo(product.get());
    }


    public String deleteProduct(String id, String requestId) {
        log.info("Deleting product by id: {} RequestId: {}",id, requestId);

        Optional<ProductEntity> product = productRepo.findById(UUID.fromString(id));
        if (product.isEmpty()) {
            log.error("Product not found with id: {} RequestId: {}", id, requestId);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found", HttpStatus.NOT_FOUND.value(), requestId);
        }

        if (product.get().getIsDeleted()) {
            log.error("Product already deleted with id: {} RequestId: {}", id, requestId);
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Product already deleted", HttpStatus.BAD_REQUEST.value(), requestId);
        }

        product.get().setIsDeleted(true);
        product.get().setCode(product.get().getCode() + "_DEL_" + requestId);
        product.get().setUpdatedAt(Instant.now());
        productRepo.save(product.get());

        log.info("Product deleted successfully with id: {} RequestId: {}", id, requestId);

        return "Product deleted successfully";
    }

    public ProductInfo updateProduct(UpdateProductRequest request, String requestId) {
        log.info("Updating product with RequestId: {} request: {}", requestId, request);

        if (request.getId().isEmpty()) {
            log.error("Product id is required | RequestId: {}", requestId);
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Product id is required", HttpStatus.BAD_REQUEST.value(), requestId);
        }

        Optional<CategoryEntity> category = categoryRepo.findById(UUID.fromString(request.getCategoryId()));
        if (category.isEmpty()) {
            log.error("Category not found with id: {} RequestId: {}", request.getCategoryId(), requestId);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found", HttpStatus.NOT_FOUND.value(), requestId);
        }

        Optional<ProductEntity> product = productRepo.findById(UUID.fromString(request.getId()));
        if (product.isEmpty()) {
            log.error("Product not found with id: {} RequestId: {}", request.getId(), requestId);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found", HttpStatus.NOT_FOUND.value(), requestId);
        }

        if (product.get().getIsDeleted()) {
            log.error("Product already deleted with id: {} RequestId: {}", request.getId(), requestId);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product already deleted", HttpStatus.NOT_FOUND.value(), requestId);
        }

        ProductEntity productEntity = productMapper.toProductEntity(request, category.get());

        productRepo.save(productEntity);

        log.info("Product updated successfully with RequestId: {} request: {}", requestId, productEntity);

        return productMapper.toProductInfo(productEntity);
    }
}
