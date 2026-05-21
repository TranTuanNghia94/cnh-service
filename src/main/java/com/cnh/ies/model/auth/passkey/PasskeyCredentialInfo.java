package com.cnh.ies.model.auth.passkey;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyCredentialInfo {
    private UUID id;
    private String deviceName;
    private String transports;
    private boolean discoverable;
    private boolean backupEligible;
    private boolean backedUp;
    private boolean active;
    private Instant createdAt;
    private Instant lastUsedAt;
}
