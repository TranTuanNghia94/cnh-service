package com.cnh.ies.service.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;

class AuthenticationUserDetailsTest {

    @Test
    void authoritiesIncludeRolesAndPermissionCodes() {
        PermissionInfo perm = new PermissionInfo();
        perm.setCode(PermissionConstants.USER_CREATE);
        perm.setDescription("Create users");
        perm.setResource("USER");
        perm.setAction("CREATE");

        RoleInfo role = new RoleInfo();
        role.setCode("ADMIN");
        role.setPermissions(Set.of(perm));

        UserInfo userInfo = new UserInfo();
        userInfo.setId(UUID.randomUUID());
        userInfo.setUsername("admin");
        userInfo.setRoles(Set.of(role));

        AuthenticationUserDetails details = new AuthenticationUserDetails(userInfo);

        Set<String> authorityNames = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorityNames.contains("ROLE_ADMIN"));
        assertTrue(authorityNames.contains(PermissionConstants.USER_CREATE));
    }
}
