package com.cnh.ies.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.model.general.GeneralRequest;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.service.user.PermissionService;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Permission", description = "Permission management APIs")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_READ + "')")
    public ApiResponse<ListDataModel<PermissionInfo>> getPermissions() {
        String requestId = RequestContext.getRequestIdOrGenerate();
        ListDataModel<PermissionInfo> response = permissionService.getAllPermissions(requestId);
        return ApiResponse.success(response, "Get all permissions success");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_READ + "')")
    public ApiResponse<PermissionInfo> getPermissionById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        PermissionInfo response = permissionService.getPermissionById(id, requestId);
        return ApiResponse.success(response, "Get permission by id success");
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> deletePermission(@RequestBody GeneralRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = permissionService.deletePermission(request.getRequestId(), requestId);
        return ApiResponse.success(response, "Delete permission success");
    }

    @PostMapping("/restore")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> restorePermission(@RequestBody GeneralRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = permissionService.restorePermission(request.getRequestId(), requestId);
        return ApiResponse.success(response, "Restore permission success");
    }
}
