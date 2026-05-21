package com.cnh.ies.model.auth;

import java.util.List;

import com.cnh.ies.model.user.PermissionInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLoginModel {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private Boolean passkeyRegistered;
    private Boolean passkeyRegistrationRequired;
    /** Flattened allowed actions for frontend (code, description, resource, action). */
    private List<PermissionInfo> permissions;
}
