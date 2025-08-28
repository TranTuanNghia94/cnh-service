package com.cnh.ies.service.user;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.user.ChangePasswordRequest;
import com.cnh.ies.model.user.CreateUserRequest;
import com.cnh.ies.model.user.UpdateUserRequest;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.util.RequestContext;
import com.cnh.ies.repository.auth.RoleRepo;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.mapper.user.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final UserMapper userMapper;
    private final RoleRepo roleRepo;

    public UserInfo getMe(String requestId) {
        try {
            String username = RequestContext.getCurrentUsername();

            log.info("Getting user info for username: {} | RequestId: {}", username, requestId);

            if (username == null) {
                log.error("Username is null | RequestId: {}", requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Username is null",
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            Optional<UserEntity> user = userRepo.findOneByUsername(username);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + username,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (!user.get().getIsActive()) {
                log.error("User is not active: {} | RequestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User is not active: " + username,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Getting user info success: {} | RequestId: {}", username, requestId);

            return userMapper.mapToUserInfo(user.get());
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }

    }

    public UserInfo getUserById(UUID id, String requestId) {
        try {
            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (user.get().getIsDeleted()) {
                log.error("User is deleted: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User is deleted: " + id,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Getting user info success: {} | RequestId: {}", id, requestId);

            return userMapper.mapToUserInfo(user.get());
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public ListDataModel<UserInfo> getUsers(String requestId, Integer page, Integer limit) {
        try {
            log.info("Start get list users with page: {} and limit: {} | RequestId: {}", page, limit, requestId);

            Page<UserEntity> users = userRepo.findAll(null, PageRequest.of(page, limit));

            List<UserInfo> userInfos = users.stream()
                    .map(userMapper::mapToUserInfo)
                    .collect(Collectors.toList());

            PaginationModel pagination = PaginationModel.builder()
                    .page(page)
                    .limit(limit)
                    .total(users.getTotalElements())
                    .totalPage(users.getTotalPages())
                    .build();

            log.info("Get list users success with page: {} and limit: {} | RequestId: {}", pagination.getPage(),
                    pagination.getLimit(), requestId);

            return ListDataModel.<UserInfo>builder()
                    .data(userInfos)
                    .pagination(pagination)
                    .build();
        } catch (Exception e) {
            log.error("Error getting users", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public UserInfo createUser(CreateUserRequest request, String requestId) {
        try {
            log.info("Start create user with request: {} | RequestId: {}", request.getUsername(), requestId);

            Optional<RoleEntity> role = roleRepo.findById(UUID.fromString(request.getRole()));

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", request.getRole(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + request.getRole(),
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            UserEntity user = userMapper.mapToUserEntity(request, role.get());

            userRepo.save(user);

            log.info("Create user success: {} | RequestId: {}", request.getUsername(), requestId);

            return userMapper.mapToUserInfo(user);
        } catch (Exception e) {
            log.error("Error creating user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public UserInfo updateUser(UpdateUserRequest request, String requestId) {
        try {
            log.info("Start update user with request: {} | RequestId: {}", request.getId(), requestId);

            Optional<UserEntity> user = userRepo.findById(UUID.fromString(request.getId()));

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", request.getId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + request.getId(),
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            Optional<RoleEntity> role = roleRepo.findById(UUID.fromString(request.getRole()));

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", request.getRole(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + request.getRole(),
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setFirstName(request.getFirstName());
            user.get().setLastName(request.getLastName());
            user.get().setPhone(request.getPhone());
            user.get().setEmail(request.getEmail());
            user.get().setRoles(new HashSet<>(Arrays.asList(role.get())));

            userRepo.save(user.get());

            log.info("Update user success: {} | RequestId: {}", request.getId(), requestId);

            return userMapper.mapToUserInfo(user.get());

        } catch (Exception e) {
            log.error("Error updating user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String deleteUser(UUID id, String requestId) {
        try {
            log.info("Start delete user with id: {} | RequestId: {}", id, requestId);

            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setIsDeleted(true);

            userRepo.save(user.get());

            log.info("Delete user success: {} | RequestId: {}", id, requestId);

            return "User deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String restoreUser(UUID id, String requestId) {
        try {
            log.info("Start restore user with id: {} | RequestId: {}", id, requestId);

            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setIsDeleted(false);

            userRepo.save(user.get());

            log.info("Restore user success: {} | RequestId: {}", id, requestId);

            return "User restored successfully";
        } catch (Exception e) {
            log.error("Error restoring user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String toggleUserActive(UUID id, String requestId) {
        try {
            log.info("Start toggle user active with id: {} | RequestId: {}", id, requestId);

            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setIsActive(!user.get().getIsActive());

            userRepo.save(user.get());

            log.info("Toggle user active success: {} | RequestId: {}", id, requestId);

            return "User active toggled successfully";
        } catch (Exception e) {
            log.error("Error toggling user active", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String changePassword(ChangePasswordRequest request, UUID id, String requestId) {
        try {
            log.info("Start change password with id: {} | RequestId: {}", id, requestId);

            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            
            if (!BCrypt.checkpw(request.getOldPassword(), user.get().getPassword())) {
                log.error("Old password is incorrect: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Old password is incorrect", HttpStatus.BAD_REQUEST.value(), requestId);
            }

            user.get().setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));

            userRepo.save(user.get());

            log.info("Change password success: {} | RequestId: {}", id, requestId);

            return "Password changed successfully";
        } catch (Exception e) {
            log.error("Error changing password", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String resetPassword(ChangePasswordRequest request, UUID id, String requestId) {
        try {
            log.info("Start reset password with id: {} | RequestId: {}", id, requestId);

            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));

            userRepo.save(user.get());

            log.info("Reset password success: {} | RequestId: {}", id, requestId);

            return "Password reset successfully";
        } catch (Exception e) {
            log.error("Error resetting password", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String assignRoleToUser(UUID userId, UUID roleId, String requestId) {
        try {
            log.info("Start assign role to user with userId: {} and roleId: {} | RequestId: {}", userId, roleId, requestId);

            Optional<UserEntity> user = userRepo.findById(userId);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", userId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + userId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            Optional<RoleEntity> role = roleRepo.findById(roleId);

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", roleId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + roleId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setRoles(new HashSet<>(Arrays.asList(role.get())));

            userRepo.save(user.get());

            log.info("Assign role to user success: {} and roleId: {} | RequestId: {}", userId, roleId, requestId);

            return "Role assigned to user successfully";
        } catch (Exception e) {
            log.error("Error assigning role to user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String unassignRoleFromUser(UUID userId, UUID roleId, String requestId) {
        try {
            log.info("Start unassign role from user with userId: {} and roleId: {} | RequestId: {}", userId, roleId, requestId);

            Optional<UserEntity> user = userRepo.findById(userId);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", userId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + userId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            user.get().setRoles(user.get().getRoles().stream().filter(role -> !role.getId().equals(roleId)).collect(Collectors.toSet()));

            userRepo.save(user.get());

            log.info("Unassign role from user success: {} and roleId: {} | RequestId: {}", userId, roleId, requestId);

            return "Role unassigned from user successfully";
        } catch (Exception e) {
            log.error("Error unassigning role from user", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
