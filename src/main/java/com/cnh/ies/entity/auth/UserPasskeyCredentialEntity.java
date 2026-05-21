package com.cnh.ies.entity.auth;

import java.time.Instant;

import com.cnh.ies.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_passkey_credentials")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPasskeyCredentialEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "credential_id", nullable = false, unique = true)
    private byte[] credentialId;

    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    private Long signatureCount = 0L;

    @Column(name = "user_handle", nullable = false)
    private byte[] userHandle;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "transports")
    private String transports;

    @Column(name = "is_discoverable", nullable = false)
    private Boolean isDiscoverable = false;

    @Column(name = "is_backup_eligible", nullable = false)
    private Boolean isBackupEligible = false;

    @Column(name = "is_backed_up", nullable = false)
    private Boolean isBackedUp = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
