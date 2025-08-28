package com.cnh.ies.service.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.RoleEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.user.RoleInfo;
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
}
