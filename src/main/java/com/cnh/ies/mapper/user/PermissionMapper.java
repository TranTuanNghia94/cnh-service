package com.cnh.ies.mapper.user;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.model.user.PermissionInfo;

@Component
public class PermissionMapper {
    public PermissionInfo mapToPermissionInfo(PermissionEntity permission) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setId(permission.getId());
        permissionInfo.setName(permission.getName());
        permissionInfo.setCode(permission.getCode());
        permissionInfo.setDescription(permission.getDescription());
        return permissionInfo;
    }
}
