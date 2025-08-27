package com.cnh.ies.model.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    @NotBlank(message = "Id is required")
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String role;
}
