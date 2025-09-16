package com.cnh.ies.repository.vendors;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.vendors.VendorBanksEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface VendorBanksRepo extends BaseRepo<VendorBanksEntity, UUID> {
    
}
