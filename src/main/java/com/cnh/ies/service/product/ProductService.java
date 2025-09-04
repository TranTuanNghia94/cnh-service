package com.cnh.ies.service.product;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.cnh.ies.repository.goods.CategoryRepo;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.mapper.product.ProductMapper;
import com.cnh.ies.repository.goods.ProductRepo;
import com.cnh.ies.model.product.ProductInfo;
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
        try {
            log.info("Getting all products with request: {}", requestId);

            Page<ProductEntity> products = productRepo.findAll(null, PageRequest.of(page, limit));
            List<ProductInfo> productInfos = products.stream().map(productMapper::toProductInfo).collect(Collectors.toList());

            PaginationModel pagination = PaginationModel.builder()
                .page(page)
                .limit(limit)
                .total(products.getTotalElements())
                .totalPage(products.getTotalPages())
                .build();

            log.info("Products fetched successfully with request: {}", requestId);

            return ListDataModel.<ProductInfo>builder()
                .data(productInfos)
                .pagination(pagination)
                .build();
        } catch (Exception e) {
            log.error("Error getting all products", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting all products", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public ProductInfo createProduct(CreateProductRequest request, String requestId) {
        try {
            log.info("Creating product: {} | RequestId: {}", request, requestId);

            CategoryEntity category = categoryRepo.findById(UUID.fromString(request.getCategoryId()))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Category not found", HttpStatus.NOT_FOUND.value(), requestId));

            ProductEntity product = productMapper.toProductEntity(request, category);

            productRepo.save(product);

            log.info("Product created successfully with request: {}", requestId);

            return productMapper.toProductInfo(product);
        } catch (Exception e) {
            log.error("Error creating product", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error creating product", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public ProductInfo getProductById(String id, String requestId) {
        try {
            log.info("Getting product by id: {}", id);

            ProductEntity product = productRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Product not found", HttpStatus.NOT_FOUND.value(), requestId));

            log.info("Product fetched successfully with id: {}", id);

            return productMapper.toProductInfo(product);
        } catch (Exception e) {
            log.error("Error getting product by id", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting product by id", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


}
