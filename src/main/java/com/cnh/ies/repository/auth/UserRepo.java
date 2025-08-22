package com.cnh.ies.repository.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.auth.UserEntity;

@Repository
public interface UserRepo extends BaseRepo<UserEntity, UUID> {

    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    Optional<UserEntity> findOneByUsername(String username);
    
}
