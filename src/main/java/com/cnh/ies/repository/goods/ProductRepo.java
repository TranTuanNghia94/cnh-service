package com.cnh.ies.repository.goods;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.product.ProductEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface ProductRepo extends BaseRepo<ProductEntity, UUID> {
    Optional<ProductEntity> findByCode(String code);
}
