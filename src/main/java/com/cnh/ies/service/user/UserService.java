package com.cnh.ies.service.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.general.ListDataModel;
import com.cnh.ies.model.general.PaginationModel;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.util.RequestContext;
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

    public UserInfo getMe(String requestId) {
        try {
            String username = RequestContext.getCurrentUsername();
    
            log.info("Getting user info for username: {} | RequestId: {}", username, requestId);

            if (username == null) {
                log.error("Username is null | RequestId: {}", requestId );
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Username is null", HttpStatus.NOT_FOUND.value(), requestId);
            }
    
            Optional<UserEntity> user = userRepo.findOneByUsername(username);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + username, HttpStatus.NOT_FOUND.value(), requestId);
            }


            if (!user.get().getIsActive()) {
                log.error("User is not active: {} | RequestId: {}", username, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User is not active: " + username, HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Getting user info success: {} | RequestId: {}", username, requestId);

            return userMapper.mapToUserInfo(user.get());
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
        
    }

    public UserInfo getUserById(UUID id, String requestId) {
        try {
            Optional<UserEntity> user = userRepo.findById(id);

            if (user.isEmpty()) {
                log.error("User not found: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }
            
            if (user.get().getIsDeleted()) {
                log.error("User is deleted: {} | RequestId: {}", id, requestId);
                throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "User is deleted: " + id, HttpStatus.NOT_FOUND.value(), requestId);
            }

            log.info("Getting user info success: {} | RequestId: {}", id, requestId);

            return userMapper.mapToUserInfo(user.get());
        } catch (Exception e) {
            log.error("Error getting user info", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
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

            log.info("Get list users success with page: {} and limit: {} | RequestId: {}", pagination.getPage(), pagination.getLimit(), requestId);

            return ListDataModel.<UserInfo>builder()
                .data(userInfos)
                .pagination(pagination)
                .build();
        } catch (Exception e) {
            log.error("Error getting users", e);
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }
}
