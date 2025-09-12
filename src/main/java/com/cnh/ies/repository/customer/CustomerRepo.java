package com.cnh.ies.repository.customer;

import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.repository.BaseRepo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CustomerRepo extends BaseRepo<CustomerEntity, UUID> {
    @Query("SELECT c FROM CustomerEntity c WHERE c.code = :code AND c.isDeleted = false")
    Optional<CustomerEntity> findByCode(String code);

    @Query("SELECT c FROM CustomerEntity c WHERE c.isDeleted = false")
    Page<CustomerEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.id = :id AND c.isDeleted = false ")
    Optional<CustomerEntity> findByIdAndIsDeletedFalse(UUID id);
}
