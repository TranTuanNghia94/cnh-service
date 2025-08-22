package com.cnh.ies.model.user;

import java.util.Set;
import java.util.UUID;

import lombok.Data;

@Data
public class UserInfo {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private Boolean isActive;
    private Set<RoleInfo> roles;
}
