package com.cnh.ies.service.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.util.PermissionUtils;

import lombok.Getter;

/**
 * {@link UserDetails} built from session {@link UserInfo} in Redis so controllers can read
 * {@link #getUserId()} without an extra database round-trip per request.
 */
@Getter
public class AuthenticationUserDetails implements UserDetails {

    private final UUID userId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticationUserDetails(UserInfo userInfo) {
        if (userInfo == null || userInfo.getId() == null) {
            throw new IllegalArgumentException("User id is required for authentication");
        }
        this.userId = userInfo.getId();
        this.username = userInfo.getUsername() != null ? userInfo.getUsername() : "";
        this.authorities = buildAuthorities(userInfo);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(UserInfo userInfo) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            for (RoleInfo role : userInfo.getRoles()) {
                if (role.getCode() != null && !role.getCode().isBlank()) {
                    String c = role.getCode().trim();
                    String roleAuthority = c.startsWith("ROLE_") ? c : "ROLE_" + c;
                    authorities.add(new SimpleGrantedAuthority(roleAuthority));
                }
            }
        }

        for (PermissionInfo permission : PermissionUtils.flattenPermissions(userInfo)) {
            if (permission.getCode() != null && !permission.getCode().isBlank()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode().trim()));
            }
        }

        if (authorities.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return List.copyOf(new ArrayList<>(authorities));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
