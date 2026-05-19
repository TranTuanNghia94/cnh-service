package com.cnh.ies.repository.customer;

import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.customer.CustomerEntity;
import com.cnh.ies.repository.BaseRepo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

@Repository
public interface CustomerRepo extends BaseRepo<CustomerEntity, UUID> {
    @Query("SELECT c FROM CustomerEntity c WHERE c.code = :code AND c.isDeleted = false")
    Optional<CustomerEntity> findByCode(String code);

    @Query("SELECT c FROM CustomerEntity c WHERE c.code IN :codes AND c.isDeleted = false")
    List<CustomerEntity> findByCodeInAndIsDeletedFalse(@Param("codes") Collection<String> codes);

    @Query("SELECT c FROM CustomerEntity c WHERE c.isDeleted = false")
    Page<CustomerEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.isDeleted = false "
            + "AND (:customerCode = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', :customerCode, '%'))) "
            + "AND (:misaCode = '' OR LOWER(COALESCE(c.misaCode, '')) LIKE LOWER(CONCAT('%', :misaCode, '%'))) "
            + "AND (:customerName = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :customerName, '%')))")
    Page<CustomerEntity> findAllFiltered(
            @Param("customerCode") String customerCode,
            @Param("misaCode") String misaCode,
            @Param("customerName") String customerName,
            Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.id = :id AND c.isDeleted = false ")
    Optional<CustomerEntity> findByIdAndIsDeletedFalse(UUID id);
}
