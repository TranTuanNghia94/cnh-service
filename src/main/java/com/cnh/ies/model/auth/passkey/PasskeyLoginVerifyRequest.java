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
public class PasskeyLoginVerifyRequest {

    @NotBlank(message = "Session id is required")
    private String sessionId;

    @NotBlank(message = "Credential response is required")
    private String credentialJson;
}
