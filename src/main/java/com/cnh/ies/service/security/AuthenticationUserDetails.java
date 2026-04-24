package com.cnh.ies.service.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cnh.ies.model.user.UserInfo;

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
        if (userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            Collection<GrantedAuthority> mapped = userInfo.getRoles().stream()
                    .filter(r -> r.getCode() != null && !r.getCode().isBlank())
                    .map(r -> {
                        String c = r.getCode().trim();
                        String role = c.startsWith("ROLE_") ? c : "ROLE_" + c;
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toUnmodifiableSet());
            this.authorities = mapped.isEmpty()
                    ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    : mapped;
        } else {
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
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
