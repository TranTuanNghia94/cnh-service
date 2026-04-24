package com.cnh.ies.repository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.repository.BaseRepo;
import com.cnh.ies.entity.auth.UserEntity;

@Repository
public interface UserRepo extends BaseRepo<UserEntity, UUID> {

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findOneByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    Optional<UserEntity> findOneByUsername(String username);

    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.code = :roleCode AND u.isActive = true AND u.isDeleted = false")
    List<UserEntity> findByRoleCode(@Param("roleCode") String roleCode);

    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.roles r WHERE r.code IN :roleCodes AND u.isActive = true AND u.isDeleted = false")
    List<UserEntity> findByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
