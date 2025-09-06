package com.cnh.ies.mapper.user;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.CreateUserRequest;
import com.cnh.ies.model.user.PermissionInfo;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    
    public UserInfo mapToUserInfo(UserEntity user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setFullName(user.getFullName());
        userInfo.setEmail(user.getEmail());
        userInfo.setIsActive(user.getIsActive());
        userInfo.setCreatedAt(user.getCreatedAt().toString());
        userInfo.setUpdatedAt(user.getUpdatedAt().toString());
        
        // Safely map roles with thread-safe collection creation
        if (user.getRoles() != null) {
            Set<RoleInfo> roleInfos = user.getRoles().stream()
                .map(this::mapToRoleInfo)
                .collect(Collectors.toSet());
            userInfo.setRoles(new HashSet<>(roleInfos));
        } else {
            userInfo.setRoles(new HashSet<>());
        }
        
        return userInfo;
    }
    
    private RoleInfo mapToRoleInfo(RoleEntity role) {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setId(role.getId());
        roleInfo.setName(role.getName());
        roleInfo.setCode(role.getCode());
        roleInfo.setDescription(role.getDescription());
        
        // Safely map permissions with thread-safe collection creation
        if (role.getPermissions() != null) {
            Set<PermissionInfo> permissionInfos = role.getPermissions().stream()
                .map(this::mapToPermissionInfo)
                .collect(Collectors.toSet());
            roleInfo.setPermissions(new HashSet<>(permissionInfos));
        } else {
            roleInfo.setPermissions(new HashSet<>());
        }
        
        return roleInfo;
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

    public UserEntity mapToUserEntity(CreateUserRequest request, RoleEntity role) {
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getLastName() + " " + request.getFirstName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsDeleted(false);
        user.setRoles(new HashSet<>(Arrays.asList(role)));

        return user;
    }
}
