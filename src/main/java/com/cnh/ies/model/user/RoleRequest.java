package com.cnh.ies.model.user;

import java.util.Optional;
import java.util.UUID;

import lombok.Data;

@Data
public class RoleRequest {
    private Optional<UUID> id;
    private String name;
    private String code;
    private String description;
}
