package com.cnh.ies.repository.vendors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;
import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface VendorsRepo extends BaseRepo<VendorsEntity, UUID> {

    @Query("SELECT v FROM VendorsEntity v WHERE v.isDeleted = false")
    Page<VendorsEntity> findAllAndIsDeletedFalse(PageRequest pageable);
    
    @Query("SELECT v FROM VendorsEntity v WHERE v.code = :code AND v.isDeleted = false")
    Optional<VendorsEntity> findByCode(String code);
}
