package com.cnh.ies.mapper.user;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.model.user.RoleInfo;

@Component
public class RoleMapper {

    public RoleInfo mapToRoleInfo(RoleEntity role) {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setId(role.getId());
        roleInfo.setName(role.getName());
        roleInfo.setCode(role.getCode());
        roleInfo.setDescription(role.getDescription());
        return roleInfo;
    }
}
