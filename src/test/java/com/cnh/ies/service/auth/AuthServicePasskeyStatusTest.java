package com.cnh.ies.service.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cnh.ies.model.auth.ResponseLoginModel;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.service.security.JwtService;
import com.cnh.ies.mapper.user.UserMapper;
import com.cnh.ies.repository.auth.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthServicePasskeyStatusTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PasskeyService passkeyService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void injectPasskeyService() {
        ReflectionTestUtils.setField(authService, "passkeyService", passkeyService);
    }

    @Test
    void issueTokens_setsPasskeyRegistrationRequiredWhenNoPasskey() {
        UUID userId = UUID.randomUUID();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername("alice");

        when(jwtService.generateAccessToken(userInfo)).thenReturn("access");
        when(passkeyService.hasActivePasskey(userId)).thenReturn(false);

        ResponseLoginModel response = authService.issueTokens(userInfo, "req-1");

        assertFalse(response.getPasskeyRegistered());
        assertTrue(response.getPasskeyRegistrationRequired());
    }

    @Test
    void issueTokens_setsPasskeyRegisteredWhenCredentialExists() {
        UUID userId = UUID.randomUUID();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername("alice");

        when(jwtService.generateAccessToken(userInfo)).thenReturn("access");
        when(passkeyService.hasActivePasskey(userId)).thenReturn(true);

        ResponseLoginModel response = authService.issueTokens(userInfo, "req-1");

        assertTrue(response.getPasskeyRegistered());
        assertFalse(response.getPasskeyRegistrationRequired());
    }
}
