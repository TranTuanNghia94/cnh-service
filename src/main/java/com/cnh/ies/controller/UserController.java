package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.user.ChangePasswordRequest;
import com.cnh.ies.model.user.CreateUserRequest;
import com.cnh.ies.model.user.UpdateUserRequest;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfo> getMe() {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting user info for me initiated requestId: {}", requestId);
        
        UserInfo response = userService.getMe(requestId);

        log.info("Getting user info for me success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Get user info success");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserInfo> getUserById(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting user info for user id: {} initiated requestId: {}", id, requestId);
        
        UserInfo response = userService.getUserById(UUID.fromString(id), requestId);

        log.info("Getting user info for user id: {} success requestId: {}", id, requestId);
        
        return ApiResponse.success(response, "Get user info success");
    }
    
    @PostMapping("/list")
    public ApiResponse<ListDataModel<UserInfo>> getUsers(@RequestBody ApiRequestModel request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting list users with page: {} and limit: {} initiated requestId: {}", request.getPage(), request.getLimit(), requestId);
        
        ListDataModel<UserInfo> response = userService.getUsers(requestId, request.getPage(), request.getLimit());

        log.info("Getting list users with page: {} and limit: {} success requestId: {}", request.getPage(), request.getLimit(), requestId);
        
        return ApiResponse.success(response, "Get list users success");
    }


    @PostMapping("/create")
    public ApiResponse<UserInfo> createUser(@RequestBody CreateUserRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Creating user with request: {} initiated requestId: {}", request.getUsername());
        
        UserInfo response = userService.createUser(request, requestId);

        log.info("Creating user success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Create user success");
    }

    @PostMapping("/update")
    public ApiResponse<UserInfo> updateUser(@RequestBody UpdateUserRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Updating user with request: {} initiated requestId: {}", request);
        
        UserInfo response = userService.updateUser(request, requestId);

        log.info("Updating user success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Update user success");
    }

    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteUser(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Deleting user with initiated requestId: {}", id);
        
        String response = userService.deleteUser(UUID.fromString(id), requestId);

        log.info("Deleting user success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Delete user success");
    }

    @PostMapping("/restore/{id}")
    public ApiResponse<String> restoreUser(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Restoring user with initiated requestId: {}", id);
        
        String response = userService.restoreUser(UUID.fromString(id), requestId);

        log.info("Restoring user success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Restore user success");
    }

    @PostMapping("/toggle-active/{id}")
    public ApiResponse<String> toggleUserActive(@PathVariable String id) {
        String requestId = UUID.randomUUID().toString();
        log.info("Toggling user active with initiated requestId: {}", id);
        
        String response = userService.toggleUserActive(UUID.fromString(id), requestId);

        log.info("Toggling user active success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Toggle user active success");
    }

    @PostMapping("/change-password/{id}")
    public ApiResponse<String> changePassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Changing password with initiated requestId: {}", requestId);

        String response = userService.changePassword(request, UUID.fromString(id), requestId);

        log.info("Changing password success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Change password success");
    }

    @PostMapping("/reset-password/{id}")
    public ApiResponse<String> resetPassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("Resetting password with initial requestId: {}", requestId);

        String response = userService.resetPassword(request, UUID.fromString(id), requestId);

        log.info("Resetting password success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Reset password success");
    }

    @PostMapping("/assign-role/{userId}/{roleId}")
    public ApiResponse<String> assignRoleToUser(@PathVariable String userId, @PathVariable String roleId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Assigning role to user with initiated requestId: {}", requestId);

        String response = userService.assignRoleToUser(UUID.fromString(userId), UUID.fromString(roleId), requestId);

        log.info("Assigning role to user success requestId: {}", requestId);

        return ApiResponse.success(response, "Assign role to user success");
    }

    @PostMapping("/unassign-role/{userId}/{roleId}")
    public ApiResponse<String> unassignRoleFromUser(@PathVariable String userId, @PathVariable String roleId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Unassigning role from user with initiated requestId: {}", requestId);

        String response = userService.unassignRoleFromUser(UUID.fromString(userId), UUID.fromString(roleId), requestId);

        log.info("Unassigning role from user success requestId: {}", requestId);

        return ApiResponse.success(response, "Unassign role from user success");
    }
}
