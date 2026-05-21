package com.cnh.ies.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;

class RoleMapperTest {

    @Test
    void mapToRoleInfo_includesAssignedPermissions() {
        PermissionEntity permission = new PermissionEntity();
        permission.setId(UUID.randomUUID());
        permission.setName("User Read");
        permission.setCode("USER_READ");
        permission.setDescription("Read users");
        permission.setResource("USER");
        permission.setAction("READ");

        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setName("Admin");
        role.setCode("ADMIN");
        role.setDescription("System administrator");
        role.setPermissions(Set.of(permission));

        RoleInfo info = new RoleMapper().mapToRoleInfo(role);

        assertNotNull(info.getPermissions());
        assertEquals(1, info.getPermissions().size());

        PermissionInfo mappedPermission = info.getPermissions().iterator().next();
        assertEquals(permission.getId(), mappedPermission.getId());
        assertEquals("USER_READ", mappedPermission.getCode());
        assertEquals("USER", mappedPermission.getResource());
        assertEquals("READ", mappedPermission.getAction());
    }
}
