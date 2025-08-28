package com.cnh.ies.service.user;

import java.util.List;
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

            List<PermissionInfo> permissionInfos = permissions.stream().map(permissionMapper::mapToPermissionInfo).collect(Collectors.toList());

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
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

}