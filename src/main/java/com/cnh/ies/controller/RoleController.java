package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.user.RoleInfo;
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
}
