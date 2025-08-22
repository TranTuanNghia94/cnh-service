package com.cnh.ies.model.user;

import lombok.Data;
import java.util.UUID;

@Data
public class PermissionInfo {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String resource;
    private String action;
}
