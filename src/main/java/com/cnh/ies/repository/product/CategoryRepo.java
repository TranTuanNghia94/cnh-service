package com.cnh.ies.repository.product;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, UUID> {

    @Query("SELECT c FROM CategoryEntity c WHERE c.code = :code")
    Optional<CategoryEntity> findByCode(String code);
    // Additional methods can be defined here if needed
}
