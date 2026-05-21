package com.cnh.ies.model.auth.passkey;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationVerifyResponse {
    private UUID credentialRecordId;
    private String deviceName;
    private boolean discoverable;
}
