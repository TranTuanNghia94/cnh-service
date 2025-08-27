package com.cnh.ies.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.user.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserInfo> getMe() {
        String requestId = UUID.randomUUID().toString();
        log.info("Getting user info for me initiated requestId: {}", requestId);
        
        UserInfo response = userService.getMe(requestId);

        log.info("Getting user info for me success requestId: {}", requestId);
        
        return ApiResponse.success(response, "Get user info success");
    }
}
