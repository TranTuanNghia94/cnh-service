package com.cnh.ies.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;

class PermissionUtilsTest {

    @Test
    void flattenPermissions_deduplicatesAcrossRoles() {
        PermissionInfo p1 = permission(PermissionConstants.USER_READ, "Read users", "USER", "READ");
        PermissionInfo p2 = permission(PermissionConstants.USER_CREATE, "Create users", "USER", "CREATE");

        RoleInfo roleA = new RoleInfo();
        roleA.setCode("ADMIN");
        roleA.setPermissions(Set.of(p1, p2));

        RoleInfo roleB = new RoleInfo();
        roleB.setCode("OTHER");
        roleB.setPermissions(Set.of(p1));

        UserInfo userInfo = new UserInfo();
        userInfo.setId(UUID.randomUUID());
        userInfo.setRoles(Set.of(roleA, roleB));

        List<PermissionInfo> flat = PermissionUtils.flattenPermissions(userInfo);

        assertEquals(2, flat.size());
        assertTrue(flat.stream().anyMatch(p -> PermissionConstants.USER_READ.equals(p.getCode())));
        assertTrue(flat.stream().anyMatch(p -> PermissionConstants.USER_CREATE.equals(p.getCode())));
        PermissionInfo read = flat.stream()
                .filter(p -> PermissionConstants.USER_READ.equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("USER", read.getResource());
        assertEquals("READ", read.getAction());
    }

    private static PermissionInfo permission(String code, String description, String resource, String action) {
        PermissionInfo p = new PermissionInfo();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setDescription(description);
        p.setResource(resource);
        p.setAction(action);
        return p;
    }
}
