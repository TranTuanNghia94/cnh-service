package com.cnh.ies.repository.auth;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.auth.UserEntity;

@Repository
public interface UserRepo extends BaseRepo<UserEntity, UUID> {
    
}
