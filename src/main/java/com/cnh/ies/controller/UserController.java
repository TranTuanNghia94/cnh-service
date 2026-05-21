package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.constant.PermissionConstants;
import com.cnh.ies.model.general.ApiRequestModel;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.model.user.ChangePasswordRequest;
import com.cnh.ies.model.user.CreateUserRequest;
import com.cnh.ies.model.user.UpdateSelfProfileRequest;
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
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_SELF_READ + "')")
    public ApiResponse<UserInfo> getMe() {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UserInfo response = userService.getMe(requestId);

        log.info("Getting user info for me success requestId: {}", requestId);

        return ApiResponse.success(response, "Get user info success");
    }

    @PostMapping("/me/profile")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_SELF_UPDATE_PROFILE + "')")
    public ApiResponse<UserInfo> updateMyProfile(@RequestBody UpdateSelfProfileRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UserInfo response = userService.updateMyProfile(request, requestId);

        log.info("Updating own profile success requestId: {}", requestId);

        return ApiResponse.success(response, "Update profile success");
    }

    @PostMapping("/me/password")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_SELF_CHANGE_PASSWORD + "')")
    public ApiResponse<String> changeMyPassword(@RequestBody ChangePasswordRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.changeMyPassword(request, requestId);

        log.info("Changing own password success requestId: {}", requestId);

        return ApiResponse.success(response, "Change password success");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_READ + "')")
    public ApiResponse<UserInfo> getUserById(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UserInfo response = userService.getUserById(UUID.fromString(id), requestId);

        log.info("Getting user info for user id: {} success requestId: {}", id, requestId);

        return ApiResponse.success(response, "Get user info success");
    }

    @PostMapping("/list")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_READ + "')")
    public ApiResponse<ListDataModel<UserInfo>> getUsers(@RequestBody ApiRequestModel request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        ListDataModel<UserInfo> response = userService.getUsers(requestId, request.getPage(), request.getLimit());

        log.info("Getting list users with page: {} and limit: {} success requestId: {}", request.getPage(),
                request.getLimit(), requestId);

        return ApiResponse.success(response, "Get list users success");
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_CREATE + "')")
    public ApiResponse<UserInfo> createUser(@RequestBody CreateUserRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UserInfo response = userService.createUser(request, requestId);

        log.info("Creating user success requestId: {}", requestId);

        return ApiResponse.success(response, "Create user success");
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<UserInfo> updateUser(@RequestBody UpdateUserRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        UserInfo response = userService.updateUser(request, requestId);

        log.info("Updating user success requestId: {}", requestId);

        return ApiResponse.success(response, "Update user success");
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_DELETE + "')")
    public ApiResponse<String> deleteUser(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.deleteUser(UUID.fromString(id), requestId);

        log.info("Deleting user success requestId: {}", requestId);

        return ApiResponse.success(response, "Delete user success");
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_RESTORE + "')")
    public ApiResponse<String> restoreUser(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.restoreUser(UUID.fromString(id), requestId);

        log.info("Restoring user success requestId: {}", requestId);

        return ApiResponse.success(response, "Restore user success");
    }

    @PostMapping("/toggle-active/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_ACTIVATE + "')")
    public ApiResponse<String> toggleUserActive(@PathVariable String id) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.toggleUserActive(UUID.fromString(id), requestId);

        log.info("Toggling user active success requestId: {}", requestId);

        return ApiResponse.success(response, "Toggle user active success");
    }

    @PostMapping("/reset-password/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_RESET_PASSWORD + "')")
    public ApiResponse<String> resetPassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        log.info("Resetting password with initial requestId: {}", requestId);

        String response = userService.resetPassword(request, UUID.fromString(id), requestId);

        log.info("Resetting password success requestId: {}", requestId);

        return ApiResponse.success(response, "Reset password success");
    }

    @PostMapping("/assign-role/{userId}/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> assignRoleToUser(@PathVariable String userId, @PathVariable String roleId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.assignRoleToUser(UUID.fromString(userId), UUID.fromString(roleId), requestId);

        log.info("Assigning role to user success requestId: {}", requestId);

        return ApiResponse.success(response, "Assign role to user success");
    }

    @PostMapping("/unassign-role/{userId}/{roleId}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.USER_UPDATE + "')")
    public ApiResponse<String> unassignRoleFromUser(@PathVariable String userId, @PathVariable String roleId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        String response = userService.unassignRoleFromUser(UUID.fromString(userId), UUID.fromString(roleId),
                requestId);

        log.info("Unassigning role from user success requestId: {}", requestId);

        return ApiResponse.success(response, "Unassign role from user success");
    }
}
