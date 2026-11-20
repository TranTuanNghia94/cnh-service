package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
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
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_READ + "')")
    public ApiResponse<ListDataModel<RoleInfo>> getRoles() {
        String requestId = RequestContext.getRequestIdOrGenerate();
        ListDataModel<RoleInfo> response = roleService.getAllRoles(requestId);
        return ApiResponse.success(response, "Get all roles success");
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<RoleInfo> createRole(@RequestBody RoleRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        RoleInfo response = roleService.createRole(request, requestId);
        return ApiResponse.success(response, "Create role success");
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<RoleInfo> updateRole(@RequestBody RoleRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        RoleInfo response = roleService.updateRole(request, requestId);
        return ApiResponse.success(response, "Update role success");
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> deleteRole(@PathVariable UUID id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = roleService.deleteRole(id, requestId);
        return ApiResponse.success(response, "Delete role success");
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> restoreRole(@PathVariable UUID id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = roleService.restoreRole(id, requestId);
        return ApiResponse.success(response, "Restore role success");
    }

    @PostMapping("/assign-permission/{roleId}/{permissionId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> assignPermissionToRole(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = roleService.assignPermissionToRole(roleId, permissionId, requestId);
        return ApiResponse.success(response, "Assign permission to role success");
    }

    @PostMapping("/unassign-permission/{roleId}/{permissionId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> unassignPermissionFromRole(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = roleService.unassignPermissionFromRole(roleId, permissionId, requestId);
        return ApiResponse.success(response, "Unassign permission from role success");
    }
}
