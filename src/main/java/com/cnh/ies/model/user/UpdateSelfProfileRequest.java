package com.cnh.ies.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSelfProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
}
