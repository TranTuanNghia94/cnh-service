package com.cnh.ies.repository.vendors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface VendorBanksRepo extends BaseRepo<VendorBanksEntity, UUID> {

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.isDeleted = false")
    Page<VendorBanksEntity> findAllAndIsDeletedFalse(PageRequest pageable);

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.vendor.id = :vendorId AND v.isDeleted = false")
    Page<VendorBanksEntity> findByVendorId(UUID vendorId, PageRequest pageable);
    
}
