package com.cnh.ies.repository.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.auth.UserPasskeyCredentialEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface UserPasskeyCredentialRepo extends BaseRepo<UserPasskeyCredentialEntity, UUID> {

    @Query("SELECT c FROM UserPasskeyCredentialEntity c WHERE c.user.id = :userId AND c.isDeleted = false AND c.isActive = true")
    List<UserPasskeyCredentialEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) > 0 FROM UserPasskeyCredentialEntity c WHERE c.user.id = :userId AND c.isDeleted = false AND c.isActive = true")
    boolean existsActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM UserPasskeyCredentialEntity c WHERE c.credentialId = :credentialId AND c.isDeleted = false AND c.isActive = true")
    Optional<UserPasskeyCredentialEntity> findActiveByCredentialId(@Param("credentialId") byte[] credentialId);

    @Query("SELECT c FROM UserPasskeyCredentialEntity c JOIN FETCH c.user WHERE c.userHandle = :userHandle AND c.isDeleted = false AND c.isActive = true")
    List<UserPasskeyCredentialEntity> findActiveByUserHandle(@Param("userHandle") byte[] userHandle);

    @Query("SELECT c FROM UserPasskeyCredentialEntity c JOIN FETCH c.user u WHERE u.username = :username AND c.isDeleted = false AND c.isActive = true")
    List<UserPasskeyCredentialEntity> findActiveByUsername(@Param("username") String username);

    @Query("SELECT c FROM UserPasskeyCredentialEntity c WHERE c.id = :id AND c.user.id = :userId AND c.isDeleted = false")
    Optional<UserPasskeyCredentialEntity> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
