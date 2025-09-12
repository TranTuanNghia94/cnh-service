package com.cnh.ies.repository.customer;

import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.customer.CustomerAddressEntity;
import com.cnh.ies.repository.BaseRepo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CustomerAddressRepo extends BaseRepo<CustomerAddressEntity, UUID> {
    @Query("SELECT c FROM CustomerAddressEntity c WHERE c.customer.id = :customerId AND c.isDeleted = false")   
    List<CustomerAddressEntity> findByCustomerId(UUID customerId);

    @Query("SELECT c FROM CustomerAddressEntity c WHERE c.isDeleted = false")
    Page<CustomerAddressEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM CustomerAddressEntity c WHERE c.customer.id = :customerId AND c.isDeleted = false")
    List<CustomerAddressEntity> findAllByCustomerId(UUID customerId);
}
