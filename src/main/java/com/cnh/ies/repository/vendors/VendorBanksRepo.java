package com.cnh.ies.repository.vendors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.repository.BaseRepo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VendorBanksRepo extends BaseRepo<VendorBanksEntity, UUID> {

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.isDeleted = false")
    Page<VendorBanksEntity> findAllAndIsDeletedFalse(PageRequest pageable);

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.vendor.id = :vendorId AND v.isDeleted = false")
    Page<VendorBanksEntity> findByVendorId(@Param("vendorId") UUID vendorId, PageRequest pageable);

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.vendor.id = :vendorId AND v.isDeleted = false")
    List<VendorBanksEntity> findByVendorAndIsDeletedFalse(@Param("vendorId") UUID vendorId);

    @Query("SELECT v FROM VendorBanksEntity v WHERE v.vendor.id IN :vendorIds AND v.isDeleted = false")
    List<VendorBanksEntity> findByVendorIdInAndIsDeletedFalse(@Param("vendorIds") List<UUID> vendorIds);
    
}
