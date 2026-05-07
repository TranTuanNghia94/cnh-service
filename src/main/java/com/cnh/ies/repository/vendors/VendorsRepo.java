package com.cnh.ies.repository.vendors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VendorsRepo extends BaseRepo<VendorsEntity, UUID> {

    @Query("SELECT DISTINCT v FROM VendorsEntity v LEFT JOIN FETCH v.banks b WHERE v.isDeleted = false")
    Page<VendorsEntity> findAllAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT v FROM VendorsEntity v WHERE v.isDeleted = false "
            + "AND (:vendorCode = '' OR LOWER(v.code) LIKE LOWER(CONCAT('%', :vendorCode, '%'))) "
            + "AND (:vendorName = '' OR LOWER(v.name) LIKE LOWER(CONCAT('%', :vendorName, '%'))) "
            + "AND (:misaCode = '' OR LOWER(COALESCE(v.misaCode, '')) LIKE LOWER(CONCAT('%', :misaCode, '%'))) "
            + "AND (:currency = '' OR LOWER(COALESCE(v.currency, '')) LIKE LOWER(CONCAT('%', :currency, '%'))) "
            + "AND (:nation = '' OR LOWER(COALESCE(v.country, '')) LIKE LOWER(CONCAT('%', :nation, '%')))")
    Page<VendorsEntity> findAllFiltered(
            @Param("vendorCode") String vendorCode,
            @Param("vendorName") String vendorName,
            @Param("misaCode") String misaCode,
            @Param("currency") String currency,
            @Param("nation") String nation,
            Pageable pageable);
    
    @Query("SELECT v FROM VendorsEntity v WHERE v.code = :code AND v.isDeleted = false")
    Optional<VendorsEntity> findByCode(String code);
    
    @Query("SELECT DISTINCT v FROM VendorsEntity v LEFT JOIN FETCH v.banks b WHERE v.id = :id AND v.isDeleted = false")
    Optional<VendorsEntity> findByIdWithBanks(UUID id);
}
