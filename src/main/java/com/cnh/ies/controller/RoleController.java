package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.model.user.RoleRequest;
import com.cnh.ies.service.user.RoleService;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role", description = "Role management APIs")
public class RoleController {
    
    
    private final RoleService roleService;

    @GetMapping("/list")
    public ApiResponse<ListDataModel<RoleInfo>> getRoles() {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all roles with initiated requestId: {}", requestId);

        ListDataModel<RoleInfo> response = roleService.getAllRoles(requestId);
        
        log.info("Getting all roles success with requestId: {}", requestId);
        
        return ApiResponse.success(response, "Get all roles success");
    }

    @PostMapping("/create")
    public ApiResponse<RoleInfo> createRole(@RequestBody RoleRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating role with initiated requestId: {}", requestId);

        RoleInfo response = roleService.createRole(request, requestId);

        log.info("Creating role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Create role success");
    }

    @PostMapping("/update")
    public ApiResponse<RoleInfo> updateRole(@RequestBody RoleRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating role with initiated requestId: {}", requestId);

        RoleInfo response = roleService.updateRole(request, requestId);

        log.info("Updating role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Update role success");
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteRole(@PathVariable UUID id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting role with initiated requestId: {}", requestId);

        String response = roleService.deleteRole(id, requestId);

        log.info("Deleting role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete role success");
    }

    @PostMapping("/restore/{id}")
    public ApiResponse<String> restoreRole(@PathVariable UUID id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Restoring role with initiated requestId: {}", requestId);

        String response = roleService.restoreRole(id, requestId);

        log.info("Restoring role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Restore role success");
    }

    @PostMapping("/assign-permission/{roleId}/{permissionId}")
    public ApiResponse<String> assignPermissionToRole(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Assigning permission to role with initiated requestId: {}", requestId);

        String response = roleService.assignPermissionToRole(roleId, permissionId, requestId);

        log.info("Assigning permission to role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Assign permission to role success");
    }

    @PostMapping("/unassign-permission/{roleId}/{permissionId}")
    public ApiResponse<String> unassignPermissionFromRole(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Unassigning permission from role with initiated requestId: {}", requestId);

        String response = roleService.unassignPermissionFromRole(roleId, permissionId, requestId);

        log.info("Unassigning permission from role success with requestId: {}", requestId);

        return ApiResponse.success(response, "Unassign permission from role success");
    }
}
