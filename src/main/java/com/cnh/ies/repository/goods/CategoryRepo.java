package com.cnh.ies.repository.goods;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.goods.CategoryEntity;
@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, UUID> {
    // Additional methods can be defined here if needed
}
