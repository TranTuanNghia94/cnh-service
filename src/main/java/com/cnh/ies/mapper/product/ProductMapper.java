package com.cnh.ies.mapper.product;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.model.product.CreateProductRequest;
import com.cnh.ies.model.product.ProductInfo;
import com.cnh.ies.entity.product.CategoryEntity;
import java.math.BigDecimal;


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
        productInfo.setCategory(product.getCategory().getName());
        productInfo.setIsActive(product.getIsActive());
        return productInfo;
    }

    public ProductEntity toProductEntity(CreateProductRequest request, CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setName(request.getName());
        product.setCode(request.getCode());
        product.setUnit1(request.getUnit1());
        product.setUnit2(request.getUnit2() == null ? null : request.getUnit2().get());
        product.setPrice(new BigDecimal(request.getPrice() == null ? 0f : request.getPrice().get()));
        product.setTax(new BigDecimal(request.getTax() == null ? 0f : request.getTax().get()));
        product.setMisaCode(request.getMisaCode() == null ? null : request.getMisaCode().get());
        product.setCostPrice(new BigDecimal(request.getCostPrice() == null ? 0f : request.getCostPrice().get()));
        product.setImageUrl(request.getImageUrl() == null ? null : request.getImageUrl().get());
        product.setDescription(request.getDescription() == null ? null : request.getDescription().get());
        product.setCategory(category);
        product.setIsActive(true);
        product.setIsDeleted(false);

        return product;
    }
}
