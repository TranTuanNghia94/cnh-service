package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.model.product.UpdateProductRequest;
import com.cnh.ies.service.product.ProductService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {
    private final ProductService productService;

    @PostMapping("/list")
    public ApiResponse<ListDataModel<ProductInfo>> getAllProducts(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all products with initiated requestId: {}", requestId);

        ListDataModel<ProductInfo> response = productService.getAllProducts(requestId, request.getPage(),
                request.getLimit());

        log.info("Getting all products success with requestId: {}", requestId);

        return ApiResponse.success(response, "Get all products success");
    }

    @PostMapping("/create")
    public ApiResponse<ProductInfo> createProduct(@RequestBody CreateProductRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating product with initiated requestId: {}", requestId);

        ProductInfo response = productService.createProduct(request, requestId);

        log.info("Creating product success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create product success");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductInfo> getProductById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting product by id: {} initiated requestId: {}", id, requestId);

        ProductInfo response = productService.getProductById(id, requestId);

        log.info("Getting product by id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get product by id success");
    }
    
    @GetMapping("/code/{code}")
    public ApiResponse<ProductInfo> getProductByCode(@PathVariable String code) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting product by code: {} initiated requestId: {}", code, requestId);

        ProductInfo response = productService.getProductByCode(code, requestId);

        log.info("Getting product by code: {} success requestId: {}", code, requestId);

        return ApiResponse.success(response, "Get product by code success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting product with initiated requestId: {}", requestId);

        String response = productService.deleteProduct(id, requestId);

        log.info("Deleting product success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete product success");
    }

    @PostMapping("/update")
    public ApiResponse<ProductInfo> updateProduct(@RequestBody UpdateProductRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating product with initiated requestId: {}", requestId);

        ProductInfo response = productService.updateProduct(request, requestId);

        log.info("Updating product success with requestId: {}", requestId);

        return ApiResponse.success(response, "Update product success");
    }
}
