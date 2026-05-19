package com.cnh.ies.repository.product;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cnh.ies.entity.product.CategoryEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CategoryRepo extends BaseRepo<CategoryEntity, UUID> {

    @Query("SELECT c FROM CategoryEntity c WHERE c.code = :code")
    Optional<CategoryEntity> findByCode(String code);

    @Query("SELECT c FROM CategoryEntity c WHERE c.code IN :codes")
    List<CategoryEntity> findByCodeIn(@Param("codes") Collection<String> codes);

    @Query("SELECT c FROM CategoryEntity c WHERE c.name = :name AND c.isDeleted = false")
    Optional<CategoryEntity> findByName(String name);
}
