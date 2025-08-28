package com.cnh.ies.service.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.user.RoleRequest;
import com.cnh.ies.model.user.RoleInfo;
import com.cnh.ies.repository.auth.PermissionRepo;
import com.cnh.ies.repository.auth.RoleRepo;
import com.cnh.ies.mapper.user.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepo roleRepo;
    private final RoleMapper roleMapper;
    private final PermissionRepo permissionRepo;


    public ListDataModel<RoleInfo> getAllRoles(String requestId) { 
        try {
            log.info("Getting all roles | RequestId: {}", requestId);

            List<RoleEntity> roles = roleRepo.findAll();

            List<RoleInfo> roleInfos = roles.stream().map(roleMapper::mapToRoleInfo).collect(Collectors.toList());

        
            PaginationModel pagination = PaginationModel.builder()
                .page(1)
                .limit(10)
                .total((long) roles.size())
                .totalPage(1)
                .build();

            log.info("Getting all roles success | RequestId: {}", requestId);

            return ListDataModel.<RoleInfo>builder()
                .data(roleInfos)
                .pagination(pagination)
                .build();

        } catch (Exception e) {
            log.error("Error getting all roles", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public RoleInfo createRole(RoleRequest request, String requestId) {
        try {
            log.info("Creating role: {} | RequestId: {}", request, requestId);

            RoleEntity role = new RoleEntity();
            role.setName(request.getName());
            role.setCode(request.getCode());
            role.setDescription(request.getDescription());

            roleRepo.save(role);

            log.info("Creating role success: {} | RequestId: {}", request, requestId);

            return roleMapper.mapToRoleInfo(role);
        } catch (Exception e) {
            log.error("Error creating role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public RoleInfo updateRole(RoleRequest request, String requestId) {
        try {
            log.info("Updating role: {} | RequestId: {}", request, requestId);

            if (request.getId().isEmpty()) {
                log.error("Role id is required | RequestId: {}", requestId);
                throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Role id is required", HttpStatus.BAD_REQUEST.value(), requestId);
            }

            Optional<RoleEntity> role = roleRepo.findById(request.getId().get());

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", request.getId(), requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + request.getId(), HttpStatus.NOT_FOUND.value(), requestId);
            }

            role.get().setName(request.getName());
            role.get().setCode(request.getCode());
            role.get().setDescription(request.getDescription());

            roleRepo.save(role.get());

            log.info("Updating role success: {} | RequestId: {}", request, requestId);

            return roleMapper.mapToRoleInfo(role.get());
        } catch (Exception e) {
            log.error("Error updating role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String deleteRole(UUID id, String requestId) {
        try {
            log.info("Deleting role: {} | RequestId: {}", id, requestId);

            Optional<RoleEntity> role = roleRepo.findById(id);

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            role.get().setCode(role.get().getCode() + "_" + "DELETED" + "_" + requestId);
            role.get().setIsDeleted(true);
            role.get().setUpdatedAt(Instant.now());
            roleRepo.save(role.get());

            log.info("Deleting role success: {} | RequestId: {}", id, requestId);

            return "Role deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String restoreRole(UUID id, String requestId) {
        try {
            log.info("Restoring role: {} | RequestId: {}", id, requestId);

            Optional<RoleEntity> role = roleRepo.findById(id);

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            role.get().setCode(role.get().getCode().replace("_DELETED_" + requestId, ""));
            role.get().setIsDeleted(false);
            role.get().setUpdatedAt(Instant.now());
            roleRepo.save(role.get());

            log.info("Restoring role success: {} | RequestId: {}", id, requestId);

            return "Role restored successfully";
        } catch (Exception e) {
            log.error("Error restoring role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String assignPermissionToRole(UUID roleId, UUID permissionId, String requestId) {
        try {
            log.info("Assigning permission: {} to role: {} | RequestId: {}", permissionId, roleId, requestId);

            Optional<RoleEntity> role = roleRepo.findById(roleId);

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", roleId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + roleId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            Optional<PermissionEntity> permission = permissionRepo.findById(permissionId);

            if (permission.isEmpty()) {
                log.error("Permission not found: {} | RequestId: {}", permissionId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission not found: " + permissionId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            role.get().getPermissions().add(permission.get());
            roleRepo.save(role.get());

            log.info("Assigning permission: {} to role: {} | RequestId: {}", permissionId, roleId, requestId);

            return "Permission assigned to role successfully";
        } catch (Exception e) {
            log.error("Error assigning permission to role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String unassignPermissionFromRole(UUID roleId, UUID permissionId, String requestId) {
        try {
            log.info("Unassigning permission: {} from role: {} | RequestId: {}", permissionId, roleId, requestId);

            Optional<RoleEntity> role = roleRepo.findById(roleId);

            if (role.isEmpty()) {
                log.error("Role not found: {} | RequestId: {}", roleId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Role not found: " + roleId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            Optional<PermissionEntity> permission = permissionRepo.findById(permissionId);

            if (permission.isEmpty()) {
                log.error("Permission not found: {} | RequestId: {}", permissionId, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission not found: " + permissionId, HttpStatus.NOT_FOUND.value(), requestId);
            }

            role.get().getPermissions().remove(permission.get());
            roleRepo.save(role.get());

            log.info("Unassigning permission: {} from role: {} | RequestId: {}", permissionId, roleId, requestId);

            return "Permission unassigned from role successfully";
        } catch (Exception e) {
            log.error("Error unassigning permission from role", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
