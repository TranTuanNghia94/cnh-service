package com.cnh.ies.model.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
