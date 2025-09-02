package com.cnh.ies.service.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.user.PermissionInfo;
import com.cnh.ies.repository.auth.PermissionRepo;
import com.cnh.ies.entity.auth.PermissionEntity;
import com.cnh.ies.mapper.user.PermissionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepo permissionRepo;
    private final PermissionMapper permissionMapper;

    public ListDataModel<PermissionInfo> getAllPermissions(String requestId) {
        try {
            log.info("Getting all permissions | RequestId: {}", requestId);

            List<PermissionEntity> permissions = permissionRepo.findAll();

            List<PermissionInfo> permissionInfos = permissions.stream().map(permissionMapper::mapToPermissionInfo)
                    .collect(Collectors.toList());

            PaginationModel pagination = PaginationModel.builder()
                    .page(1)
                    .limit(10)
                    .total((long) permissions.size())
                    .totalPage(1)
                    .build();

            log.info("Getting all permissions success | RequestId: {}", requestId);

            return ListDataModel.<PermissionInfo>builder()
                    .data(permissionInfos)
                    .pagination(pagination)
                    .build();

        } catch (Exception e) {
            log.error("Error getting all permissions", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public PermissionInfo getPermissionById(String id, String requestId) {
        try {
            log.info("Getting permission by id: {}", id);

            Optional<PermissionEntity> permission = permissionRepo.findById(UUID.fromString(id));

            if (permission.isEmpty()) {
                log.error("Permission not found with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission not found with id: " + id,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            if (permission.get().getIsDeleted()) {
                log.error("Permission is deleted with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission is deleted with id: " + id,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Permission fetched successfully with id: {}", id);

            return permissionMapper.mapToPermissionInfo(permission.get());
        } catch (Exception e) {
            log.error("Error getting permission by id", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error getting permission by id",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String deletePermission(String id, String requestId) {
        try {
            log.info("Deleting permission with id: {}", id);

            Optional<PermissionEntity> permission = permissionRepo.findById(UUID.fromString(id));

            if (permission.isEmpty()) {
                log.error("Permission not found with id: {}", id);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission not found with id: " + id,
                        HttpStatus.NOT_FOUND.value(), requestId);
            }

            permission.get().setIsDeleted(true);
            permission.get().setCode(permission.get().getCode() + "_" + "DELETED" + "_" + requestId);
            permissionRepo.save(permission.get());

            log.info("Permission deleted successfully with id: {}", id);

            return "Permission deleted successfully";
        } catch (Exception e) {
            log.error("Error deleting permission", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error deleting permission",
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public String restorePermission(String id, String requestId) {
        try {
            log.info("Restoring permission with id: {}", id);

        Optional<PermissionEntity> permission = permissionRepo.findById(UUID.fromString(id));

        if (permission.isEmpty()) {
            log.error("Permission not found with id: {}", id);
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Permission not found with id: " + id, HttpStatus.NOT_FOUND.value(), requestId);
        }
        
        permission.get().setCode(permission.get().getCode().replace("_DELETED_" + requestId, ""));
        permission.get().setIsDeleted(false);
        permissionRepo.save(permission.get());
        
        log.info("Permission restored successfully with id: {}", id);

        return "Permission restored successfully";
        } catch (Exception e) {
            log.error("Error restoring permission", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, "Error restoring permission", HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}