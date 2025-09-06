package com.cnh.ies.controller;

import java.util.UUID;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.auth.LoginModel;
import com.cnh.ies.model.auth.ResponseLoginModel;
import com.cnh.ies.service.auth.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<ResponseLoginModel> login(@RequestBody LoginModel payload) {
        String requestId = UUID.randomUUID().toString();
        log.info("Login request initiated: {} | RequestId: {}",payload.getEmail(), requestId);
        
        ResponseLoginModel response = authService.login(payload, requestId);

        log.info("Login request completed: {} | RequestId: {}",payload.getEmail(), requestId);
        
        return ApiResponse.success(response, "Login success");
    }

    @PostMapping("/refresh-token")
    public ApiResponse<ResponseLoginModel> refreshToken(@RequestBody String refreshToken) {
        String requestId = UUID.randomUUID().toString();
        log.info("Refresh token request initiated: {} | RequestId: {}", refreshToken, requestId);
        
        ResponseLoginModel response = authService.refreshToken(refreshToken, requestId);

        log.info("Refresh token request completed: {} | RequestId: {}", refreshToken, requestId);
        
        return ApiResponse.success(response, "Refresh token success");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        String requestId = UUID.randomUUID().toString();
        log.info("Logout request initiated: | RequestId: {}", requestId);
        
        String response = authService.logout(requestId);

        log.info("Logout request completed: | RequestId: {}", requestId);

        return ApiResponse.success(response, "Logout success");
    }
}
