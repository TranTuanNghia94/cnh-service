package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.dto.common.ApiResponse;
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
    public ApiResponse<ListDataModel<PermissionInfo>> getPermissions() {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting all permissions with initiated requestId: {}", requestId);

        ListDataModel<PermissionInfo> response = permissionService.getAllPermissions(requestId);

        log.info("Getting all permissions success with requestId: {}", requestId);

        return ApiResponse.success(response, "Get all permissions success");
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionInfo> getPermissionById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting permission by id with initiated requestId: {}", requestId);

        PermissionInfo response = permissionService.getPermissionById(id, requestId);

        log.info("Getting permission by id success with requestId: {}", requestId);

        return ApiResponse.success(response, "Get permission by id success");

    }

    @PostMapping("/delete")
    public ApiResponse<String> deletePermission(@RequestBody GeneralRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting permission with initiated requestId: {}", requestId);

        String response = permissionService.deletePermission(request.getRequestId(), requestId);

        log.info("Deleting permission success with requestId: {}", requestId);

        return ApiResponse.success(response, "Delete permission success");

    }

    @PostMapping("/restore")
    public ApiResponse<String> restorePermission(@RequestBody GeneralRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Restoring permission with initiated requestId: {}", requestId);

        String response = permissionService.restorePermission(request.getRequestId(), requestId);

        log.info("Restoring permission success with requestId: {}", requestId);

        return ApiResponse.success(response, "Restore permission success");
    }

}
