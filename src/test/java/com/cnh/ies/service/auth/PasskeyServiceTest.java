package com.cnh.ies.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cnh.ies.config.WebAuthnProperties;
import com.cnh.ies.constant.RedisKey;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.user.UserMapper;
import com.cnh.ies.model.auth.passkey.PasskeyLoginOptionsRequest;
import com.cnh.ies.model.auth.passkey.PasskeyLoginVerifyRequest;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.repository.auth.UserPasskeyCredentialRepo;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.service.security.AuthenticationUserDetails;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;

@ExtendWith(MockitoExtension.class)
class PasskeyServiceTest {

    @Mock
    private RelyingParty relyingParty;
    @Mock
    private RedisService redisService;
    @Mock
    private UserRepo userRepo;
    @Mock
    private UserPasskeyCredentialRepo passkeyCredentialRepo;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private WebAuthnProperties webAuthnProperties;

    @InjectMocks
    private PasskeyService passkeyService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.clearContext();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setUsername("alice");
        AuthenticationUserDetails principal = new AuthenticationUserDetails(userInfo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void hasActivePasskey_delegatesToRepository() {
        when(passkeyCredentialRepo.existsActiveByUserId(userId)).thenReturn(true);
        assertEquals(true, passkeyService.hasActivePasskey(userId));
    }

    @Test
    void hasActivePasskey_returnsFalseWhenNone() {
        when(passkeyCredentialRepo.existsActiveByUserId(userId)).thenReturn(false);
        assertFalse(passkeyService.hasActivePasskey(userId));
    }

    @Test
    void finishLogin_throwsWhenChallengeMissing() {
        PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder()
                .sessionId("session-1")
                .credentialJson("{}")
                .build();

        when(redisService.get(RedisKey.PASSKEY_LOGIN_CHALLENGE_PREFIX + "session-1")).thenReturn(null);

        assertThrows(ApiException.class, () -> passkeyService.finishLogin(request, "req-1"));
    }

    @Test
    void startLogin_storesChallengeInRedisWithSessionId() throws Exception {
        when(webAuthnProperties.getChallengeTtlSeconds()).thenReturn(300);
        AssertionRequest assertionRequest = AssertionRequest.builder()
                .publicKeyCredentialRequestOptions(
                        com.yubico.webauthn.data.PublicKeyCredentialRequestOptions.builder()
                                .challenge(new com.yubico.webauthn.data.ByteArray(new byte[32]))
                                .rpId("localhost")
                                .build())
                .build();
        when(relyingParty.startAssertion(any(StartAssertionOptions.class))).thenReturn(assertionRequest);

        var response = passkeyService.startLogin(new PasskeyLoginOptionsRequest(), "req-1");

        assertEquals(assertionRequest.toCredentialsGetJson(), response.getOptionsJson());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisService).set(keyCaptor.capture(), eq(assertionRequest.toJson()), eq(Duration.ofSeconds(300)));
        assertEquals(RedisKey.PASSKEY_LOGIN_CHALLENGE_PREFIX + response.getSessionId(), keyCaptor.getValue());
    }

    @Test
    void finishRegistration_throwsWhenChallengeMissing() {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("alice");
        user.setIsActive(true);
        user.setIsDeleted(false);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(redisService.get(RedisKey.PASSKEY_REGISTER_CHALLENGE_PREFIX + "alice")).thenReturn(null);

        var request = com.cnh.ies.model.auth.passkey.PasskeyRegistrationVerifyRequest.builder()
                .credentialJson("{}")
                .build();

        assertThrows(ApiException.class, () -> passkeyService.finishRegistration(request, "req-1"));
    }
}
