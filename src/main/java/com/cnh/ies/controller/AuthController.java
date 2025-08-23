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
        UUID requestId = UUID.randomUUID();
        log.info("Login request initiated: {} | RequestId: {}",payload.getUsername(), requestId);
        
        ResponseLoginModel response = authService.login(payload, null);

        log.info("Login request completed: {} | RequestId: {}",payload.getUsername(), requestId);
        
        return ApiResponse.success(response, "Login success");
    }
  
}
