package com.cnh.ies.mapper.product;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.model.product.UpdateProductRequest;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.entity.product.CategoryEntity;
import java.math.BigDecimal;
import java.util.UUID;


@Component
public class ProductMapper {
    public ProductInfo toProductInfo(ProductEntity product) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setId(product.getId().toString());
        productInfo.setName(product.getName());
        productInfo.setCode(product.getCode());
        productInfo.setUnit1(product.getUnit1());
        productInfo.setUnit2(product.getUnit2() == null ? null : product.getUnit2());
        productInfo.setPrice(product.getPrice().toString());
        productInfo.setTax(product.getTax().toString());
        productInfo.setMisaCode(product.getMisaCode() == null ? null : product.getMisaCode());
        productInfo.setCostPrice(product.getCostPrice().toString());
        productInfo.setImageUrl(product.getImageUrl() == null ? null : product.getImageUrl());
        productInfo.setDescription(product.getDescription() == null ? null : product.getDescription());
        productInfo.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
        productInfo.setCategoryId(product.getCategory() == null ? null : product.getCategory().getId().toString());
        productInfo.setIsActive(product.getIsActive());
        return productInfo;
    }

    public ProductEntity toProductEntity(CreateProductRequest request, CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setName(request.getName().trim());
        product.setCode(request.getCode().trim().toUpperCase());
        product.setUnit1(request.getUnit1().trim());
        product.setUnit2(request.getUnit2() == null ? null : request.getUnit2().get());
        product.setPrice(new BigDecimal(request.getPrice() == null ? 0f : request.getPrice().get()));
        product.setTax(new BigDecimal(request.getTax() == null ? 0f : request.getTax().get()));
        product.setMisaCode(request.getMisaCode() == null ? null : request.getMisaCode().get().trim().toUpperCase());
        product.setCostPrice(new BigDecimal(request.getCostPrice() == null ? 0f : request.getCostPrice().get()));
        product.setImageUrl(request.getImageUrl() == null ? null : request.getImageUrl().get().trim());
        product.setDescription(request.getDescription() == null ? null : request.getDescription().get().trim());
        product.setCategory(category);
        product.setCreatedBy(RequestContext.getCurrentUsername());
        product.setUpdatedBy(RequestContext.getCurrentUsername());
        product.setIsActive(true);
        product.setIsDeleted(false);

        return product;
    }

    public ProductEntity toProductEntity(UpdateProductRequest request, CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setId(UUID.fromString(request.getId()));
        product.setName(request.getName());
        product.setCode(request.getCode());
        product.setUnit1(request.getUnit1());
        product.setCategory(category);
        product.setUnit2(request.getUnit2() == null ? null : request.getUnit2().get());
        product.setPrice(new BigDecimal(request.getPrice() == null ? 0f : request.getPrice().get()));
        product.setTax(new BigDecimal(request.getTax() == null ? 0f : request.getTax().get()));
        product.setMisaCode(request.getMisaCode() == null ? null : request.getMisaCode().get().trim().toUpperCase());
        product.setCostPrice(new BigDecimal(request.getCostPrice() == null ? 0f : request.getCostPrice().get()));
        product.setImageUrl(request.getImageUrl() == null ? null : request.getImageUrl().get().trim());
        product.setDescription(request.getDescription() == null ? null : request.getDescription().get().trim());
        product.setUpdatedBy(RequestContext.getCurrentUsername());
        product.setIsActive(request.getIsActive());
        
        return product;
    }
}
