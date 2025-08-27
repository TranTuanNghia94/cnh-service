package com.cnh.ies.service.user;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.exception.ApiException;
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

    
}
