package com.cnh.ies.model.auth.passkey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyRegistrationOptionsResponse {
    private String optionsJson;
}
