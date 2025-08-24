package com.cnh.ies.entity.auth;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

import com.cnh.ies.entity.BaseEntity;

@Data
@Entity
@Table(name = "permissions")
@EqualsAndHashCode(callSuper = true)
public class PermissionEntity extends BaseEntity {

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "resource", length = 100)
    private String resource;

    @Column(name = "action", length = 100)
    private String action;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Set<RoleEntity> roles = new HashSet<>();
}
