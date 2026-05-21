package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.dto.response.UploadOjectResponse;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.model.product.ProductListRequest;
import com.cnh.ies.model.product.ProductTaxHistoryInfo;
import com.cnh.ies.model.product.UpdateProductRequest;
import com.cnh.ies.service.product.ProductService;
import com.cnh.ies.service.product.UploadProductService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/product")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {
    private final ProductService productService;
    private final UploadProductService uploadProductService;

    @PostMapping("/list")
    public ApiResponse<ListDataModel<ProductInfo>> getAllProducts(@RequestBody ProductListRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ListDataModel<ProductInfo> response = productService.getAllProducts(
                requestId,
                request.getPage(),
                request.getLimit(),
                request.getProductCode(),
                request.getProductName(),
                request.getProductCategory());
return ApiResponse.success(response, "Get all products success");
    }

    @PostMapping("/create")
    public ApiResponse<ProductInfo> createProduct(@RequestBody CreateProductRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ProductInfo response = productService.createProduct(request, requestId);
return ApiResponse.success(response, "Create product success");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductInfo> getProductById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ProductInfo response = productService.getProductById(id, requestId);

        log.info("Getting product by id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get product by id success");
    }

    @GetMapping("/{id}/tax-history")
    public ApiResponse<ListDataModel<ProductTaxHistoryInfo>> getProductTaxHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        ListDataModel<ProductTaxHistoryInfo> response =
                productService.getProductTaxHistory(id, page, limit, requestId);
        return ApiResponse.success(response, "Get product tax history success");
    }
    
    @GetMapping("/code/{code}")
    public ApiResponse<ProductInfo> getProductByCode(@PathVariable String code) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ProductInfo response = productService.getProductByCode(code, requestId);

        log.info("Getting product by code: {} success requestId: {}", code, requestId);

        return ApiResponse.success(response, "Get product by code success");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
String response = productService.deleteProduct(id, requestId);
return ApiResponse.success(response, "Delete product success");
    }

    @PostMapping("/update")
    public ApiResponse<ProductInfo> updateProduct(@RequestBody UpdateProductRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
ProductInfo response = productService.updateProduct(request, requestId);
return ApiResponse.success(response, "Update product success");
    }

    @PostMapping("/upload-file-product")
    public ApiResponse<UploadOjectResponse> uploadFileProduct(@RequestParam("file") MultipartFile file) {
        String requestId = RequestContext.getRequestIdOrGenerate();
UploadOjectResponse response = uploadProductService.readExcelFile(file, requestId);
return ApiResponse.success(response, "Upload file product success");
    }
}
