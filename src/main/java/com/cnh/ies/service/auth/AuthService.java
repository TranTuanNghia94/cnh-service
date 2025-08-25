package com.cnh.ies.service.auth;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;


import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.constant.RedisKey;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.auth.LoginModel;
import com.cnh.ies.model.auth.ResponseLoginModel;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.mapper.user.UserMapper;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.service.security.JwtService;
import com.cnh.ies.util.RequestContext;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public ResponseLoginModel login(LoginModel payload, String requestId) {
        try {
            log.info("Login request: {} | RequestId: {}", payload.getUsername(), requestId);
            
            Optional<UserEntity> user = userRepo.findOneByUsername(payload.getUsername());

            if (user.isEmpty() || !user.get().getIsActive()) {
                log.error("User not found: {} | RequestId: {}", payload.getUsername(), requestId);
                throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "User not found",
                HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            UserInfo userInfo = userMapper.mapToUserInfo(user.get());

            if (!BCrypt.checkpw(payload.getPassword(), user.get().getPassword())) {
                log.error("Invalid username or password: {} | RequestId: {}", payload.getUsername(), requestId);
                throw new ApiException(ApiException.ErrorCode.INVALID_CREDENTIALS, "Invalid username or password",
                HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            String accessToken = jwtService.generateAccessToken(userInfo);
            String refreshToken = generateRefreshToken();

            storeUserTokens(userInfo, accessToken, refreshToken);

            log.info("Login success: {} | RequestId: {}", userInfo, requestId);

            return ResponseLoginModel.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(userInfo.getUsername())
                .tokenType("Bearer")
                .build();

        } catch (Exception e) {
            log.error("Error logging in: {}", e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }

    public ResponseLoginModel refreshToken(String refreshToken, String requestId) {
        try {
            log.info("Refresh token request: {} | RequestId: {}", refreshToken, requestId);

            if (refreshToken == null) {
                log.error("Refresh token is required: {} | RequestId: {}", refreshToken, requestId);
                throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "Refresh token is required",
                HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            String username =  RequestContext.getCurrentUsername();

            if (!isValidRefreshToken(username, refreshToken)) {
                log.error("Invalid refresh token: {} | RequestId: {}", refreshToken, requestId);
                throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "Invalid refresh token",
                HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            UserInfo userInfo = getUserInfoFromRedis(username);

            String accessToken = jwtService.generateAccessToken(userInfo);
            String newRefreshToken = generateRefreshToken();

            storeUserTokens(userInfo, accessToken, newRefreshToken);

            log.info("Refresh token success: {} | RequestId: {}", userInfo, requestId);

            return ResponseLoginModel.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .username(userInfo.getUsername())
                .tokenType("Bearer")
                .build();

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
        }
    }


    private void storeUserTokens(UserInfo userInfo, String accessToken, String refreshToken) {
        String username = userInfo.getUsername();
        redisService.set(RedisKey.REFRESH_TOKEN_PREFIX + username, refreshToken, RedisKey.REFRESH_TOKEN_DURATION);
        redisService.set(RedisKey.ACCESS_TOKEN_PREFIX + username, accessToken, RedisKey.ACCESS_TOKEN_DURATION);
        redisService.set(username, userInfo, RedisKey.USER_INFO_DURATION);
    }

    private void removeUserTokens(String username) {
        redisService.delete(RedisKey.REFRESH_TOKEN_PREFIX + username);
        redisService.delete(RedisKey.ACCESS_TOKEN_PREFIX + username);
        redisService.delete(username);
    }

    private boolean isValidRefreshToken(String username, String refreshToken) {
        Object user = redisService.get(username);
        String redisRefreshToken = (String) redisService.get(RedisKey.REFRESH_TOKEN_PREFIX + username);
        return user != null && redisRefreshToken != null && redisRefreshToken.equals(refreshToken);
    }

    private UserInfo getUserInfoFromRedis(String username) {
        Object user = redisService.get(username);
        return objectMapper.convertValue(user, UserInfo.class);
    }

    private String generateRefreshToken() {
        return Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
    }
}
