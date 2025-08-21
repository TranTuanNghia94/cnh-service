package com.cnh.ies.repository.auth;

import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.auth.RoleEntity;

@Repository
public interface RoleRepo extends BaseRepo<RoleEntity, UUID> {
    
}
