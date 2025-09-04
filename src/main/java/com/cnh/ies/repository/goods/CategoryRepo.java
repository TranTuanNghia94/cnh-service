package com.cnh.ies.repository.goods;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.repository.BaseRepo;
@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, UUID> {
    // Additional methods can be defined here if needed
}
