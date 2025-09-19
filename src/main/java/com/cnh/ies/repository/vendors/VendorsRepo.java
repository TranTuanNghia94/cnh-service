package com.cnh.ies.repository.vendors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface VendorsRepo extends BaseRepo<VendorsEntity, UUID> {

    @Query("SELECT DISTINCT v FROM VendorsEntity v LEFT JOIN FETCH v.banks b WHERE v.isDeleted = false")
    Page<VendorsEntity> findAllAndIsDeletedFalse(Pageable pageable);
    
    @Query("SELECT v FROM VendorsEntity v WHERE v.code = :code AND v.isDeleted = false")
    Optional<VendorsEntity> findByCode(String code);
    
    @Query("SELECT DISTINCT v FROM VendorsEntity v LEFT JOIN FETCH v.banks b WHERE v.id = :id AND v.isDeleted = false")
    Optional<VendorsEntity> findByIdWithBanks(UUID id);
}
