package com.cnh.ies.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.model.user.PermissionInfo;

class PermissionMapperTest {

    @Test
    void mapToPermissionInfo_includesResourceAndAction() {
        PermissionEntity entity = new PermissionEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("User Create");
        entity.setCode("USER_CREATE");
        entity.setDescription("Create users");
        entity.setResource("USER");
        entity.setAction("CREATE");

        PermissionMapper mapper = new PermissionMapper();
        PermissionInfo info = mapper.mapToPermissionInfo(entity);

        assertEquals("USER", info.getResource());
        assertEquals("CREATE", info.getAction());
        assertEquals("Create users", info.getDescription());
    }
}
