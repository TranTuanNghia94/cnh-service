package com.cnh.ies.model.user;

import lombok.Data;
import java.util.Set;
import java.util.UUID;


@Data
public class RoleInfo {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private Set<PermissionInfo> permissions;
}
