package com.cnh.ies.repository.vendors;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.entity.vendors.VendorsEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface VendorsRepo extends BaseRepo<VendorsEntity, UUID> {
    
}
