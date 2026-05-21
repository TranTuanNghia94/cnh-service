package com.cnh.ies.mapper.user;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;

@Component
public class RoleMapper {

    public RoleInfo mapToRoleInfo(RoleEntity role) {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setId(role.getId());
        roleInfo.setName(role.getName());
        roleInfo.setCode(role.getCode());
        roleInfo.setDescription(role.getDescription());
        roleInfo.setPermissions(mapPermissions(role.getPermissions()));
        return roleInfo;
    }

    private Set<PermissionInfo> mapPermissions(Set<PermissionEntity> permissions) {
        if (permissions == null) {
            return new HashSet<>();
        }

        return permissions.stream()
                .map(this::mapToPermissionInfo)
                .collect(Collectors.toSet());
    }

    private PermissionInfo mapToPermissionInfo(PermissionEntity permission) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setId(permission.getId());
        permissionInfo.setName(permission.getName());
        permissionInfo.setCode(permission.getCode());
        permissionInfo.setDescription(permission.getDescription());
        permissionInfo.setResource(permission.getResource());
        permissionInfo.setAction(permission.getAction());
        return permissionInfo;
    }
}
