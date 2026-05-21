package com.cnh.ies.model.auth.passkey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyLoginOptionsRequest {
    /** Optional: limits allowCredentials to a known user (email or username). */
    private String identifier;
}
