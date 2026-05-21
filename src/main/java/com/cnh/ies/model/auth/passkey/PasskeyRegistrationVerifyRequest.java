package com.cnh.ies.model.auth.passkey;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationVerifyRequest {

    @NotBlank(message = "Credential response is required")
    private String credentialJson;

    private String deviceName;
}
