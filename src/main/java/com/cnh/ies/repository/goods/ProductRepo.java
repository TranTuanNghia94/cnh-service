package com.cnh.ies.repository.goods;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.goods.ProductEntity;

@Repository
public interface ProductRepo extends BaseRepo<ProductEntity, UUID> {
    
}
