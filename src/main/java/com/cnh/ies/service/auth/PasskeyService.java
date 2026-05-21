package com.cnh.ies.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cnh.ies.config.WebAuthnProperties;
import com.cnh.ies.constant.RedisKey;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.auth.UserPasskeyCredentialEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.user.UserMapper;
import com.cnh.ies.model.auth.ResponseLoginModel;
import com.cnh.ies.model.auth.passkey.PasskeyAssertionOptionsResponse;
import com.cnh.ies.model.auth.passkey.PasskeyCredentialInfo;
import com.cnh.ies.model.auth.passkey.PasskeyLoginOptionsRequest;
import com.cnh.ies.model.auth.passkey.PasskeyLoginVerifyRequest;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationOptionsResponse;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationVerifyRequest;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationVerifyResponse;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.repository.auth.UserPasskeyCredentialRepo;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.service.redis.RedisService;
import com.cnh.ies.service.security.AuthenticationUserDetails;
import com.cnh.ies.util.PasskeyUserHandle;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasskeyService {

    private final RelyingParty relyingParty;
    private final RedisService redisService;
    private final UserRepo userRepo;
    private final UserPasskeyCredentialRepo passkeyCredentialRepo;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final WebAuthnProperties webAuthnProperties;

    public boolean hasActivePasskey(UUID userId) {
        return passkeyCredentialRepo.existsActiveByUserId(userId);
    }

    public PasskeyRegistrationOptionsResponse startRegistration(String requestId) {
        try {
            UserEntity user = requireCurrentUser(requestId);
            UserIdentity userIdentity = buildUserIdentity(user);

            PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
                    StartRegistrationOptions.builder()
                            .user(userIdentity)
                            .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                    .residentKey(ResidentKeyRequirement.PREFERRED)
                                    .userVerification(UserVerificationRequirement.REQUIRED)
                                    .build())
                            .build());

            String redisKey = registerChallengeKey(user.getUsername());
            redisService.set(redisKey, options.toJson(), challengeDuration());

            return PasskeyRegistrationOptionsResponse.builder()
                    .optionsJson(options.toCredentialsCreateJson())
                    .build();
        } catch (Exception e) {
            throw webAuthnException("Failed to start passkey registration", e, requestId);
        }
    }

    @Transactional
    public PasskeyRegistrationVerifyResponse finishRegistration(
            PasskeyRegistrationVerifyRequest payload, String requestId) {
        UserEntity user = requireCurrentUser(requestId);
        String redisKey = registerChallengeKey(user.getUsername());
        String optionsJson = getChallengeOrThrow(redisKey, requestId);
        redisService.delete(redisKey);

        try {
            PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.fromJson(optionsJson);
            var pkc = PublicKeyCredential.parseRegistrationResponseJson(payload.getCredentialJson());
            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(pkc)
                    .build());

            UserPasskeyCredentialEntity entity = new UserPasskeyCredentialEntity();
            entity.setUser(user);
            entity.setCredentialId(result.getKeyId().getId().getBytes());
            entity.setPublicKeyCose(result.getPublicKeyCose().getBytes());
            entity.setSignatureCount(result.getSignatureCount());
            entity.setUserHandle(PasskeyUserHandle.fromUserId(user.getId()));
            entity.setDeviceName(payload.getDeviceName() != null ? payload.getDeviceName() : "Passkey");
            entity.setTransports(formatTransports(pkc.getResponse().getTransports()));
            entity.setIsDiscoverable(result.isDiscoverable().orElse(false));
            entity.setIsBackupEligible(result.isBackupEligible());
            entity.setIsBackedUp(result.isBackedUp());
            entity.setIsActive(true);
            entity.setCreatedBy(user.getUsername());

            UserPasskeyCredentialEntity saved = passkeyCredentialRepo.save(entity);

            log.info("Passkey registered for user {} | RequestId: {}", user.getUsername(), requestId);

            return PasskeyRegistrationVerifyResponse.builder()
                    .credentialRecordId(saved.getId())
                    .deviceName(saved.getDeviceName())
                    .discoverable(saved.getIsDiscoverable())
                    .build();
        } catch (RegistrationFailedException e) {
            log.warn("Passkey registration failed for {}: {} | RequestId: {}", user.getUsername(), e.getMessage(), requestId);
            throw new ApiException(ApiException.ErrorCode.VALIDATION_ERROR, "Passkey registration failed: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST.value(), requestId);
        } catch (Exception e) {
            throw webAuthnException("Passkey registration failed", e, requestId);
        }
    }

    public List<PasskeyCredentialInfo> listCredentials(String requestId) {
        UserEntity user = requireCurrentUser(requestId);
        return passkeyCredentialRepo.findActiveByUserId(user.getId()).stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCredential(UUID credentialId, String requestId) {
        UserEntity user = requireCurrentUser(requestId);
        UserPasskeyCredentialEntity entity = passkeyCredentialRepo.findByIdAndUserId(credentialId, user.getId())
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Passkey not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
        entity.setIsActive(false);
        entity.setIsDeleted(true);
        entity.setUpdatedBy(user.getUsername());
        passkeyCredentialRepo.save(entity);
        log.info("Passkey {} disabled for user {} | RequestId: {}", credentialId, user.getUsername(), requestId);
    }

    public PasskeyAssertionOptionsResponse startLogin(PasskeyLoginOptionsRequest payload, String requestId) {
        StartAssertionOptions.StartAssertionOptionsBuilder builder = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED);

        if (payload != null && payload.getIdentifier() != null && !payload.getIdentifier().isBlank()) {
            UserEntity user = resolveUserByIdentifier(payload.getIdentifier().trim(), requestId);
            builder.username(user.getUsername());
        }

        try {
            AssertionRequest request = relyingParty.startAssertion(builder.build());
            String sessionId = UUID.randomUUID().toString();
            String sessionKey = loginChallengeKey(sessionId);
            redisService.set(sessionKey, request.toJson(), challengeDuration());

            return PasskeyAssertionOptionsResponse.builder()
                    .sessionId(sessionId)
                    .optionsJson(request.toCredentialsGetJson())
                    .build();
        } catch (Exception e) {
            throw webAuthnException("Failed to start passkey login", e, requestId);
        }
    }

    public ResponseLoginModel finishLogin(PasskeyLoginVerifyRequest payload, String requestId) {
        String sessionKey = loginChallengeKey(payload.getSessionId());
        String optionsJson = getChallengeOrThrow(sessionKey, requestId);
        redisService.delete(sessionKey);

        try {
            AssertionRequest request = AssertionRequest.fromJson(optionsJson);
            var pkc = PublicKeyCredential.parseAssertionResponseJson(payload.getCredentialJson());
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());

            if (!result.isSuccess()) {
                throw new ApiException(ApiException.ErrorCode.INVALID_CREDENTIALS, "Passkey authentication failed",
                        HttpStatus.UNAUTHORIZED.value(), requestId);
            }

            UserEntity user = userRepo.findOneByUsername(result.getUsername())
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getIsDeleted()))
                    .orElseThrow(() -> new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "User not found",
                            HttpStatus.UNAUTHORIZED.value(), requestId));

            updateCredentialAfterAssertion(result);

            UserInfo userInfo = userMapper.mapToUserInfo(user);
            log.info("Passkey login success for {} | RequestId: {}", user.getUsername(), requestId);
            return authService.issueTokens(userInfo, requestId);
        } catch (AssertionFailedException e) {
            log.warn("Passkey login failed: {} | RequestId: {}", e.getMessage(), requestId);
            throw new ApiException(ApiException.ErrorCode.INVALID_CREDENTIALS, "Passkey authentication failed: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED.value(), requestId);
        } catch (Exception e) {
            throw webAuthnException("Passkey login failed", e, requestId);
        }
    }

    private ApiException webAuthnException(String message, Exception cause, String requestId) {
        log.warn("{}: {} | RequestId: {}", message, cause.getMessage(), requestId);
        return new ApiException(ApiException.ErrorCode.INTERNAL_ERROR, message + ": " + cause.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(), requestId);
    }

    private void updateCredentialAfterAssertion(AssertionResult result) {
        passkeyCredentialRepo.findActiveByCredentialId(result.getCredentialId().getBytes())
                .ifPresent(entity -> {
                    entity.setSignatureCount(result.getSignatureCount());
                    entity.setIsBackedUp(result.isBackedUp());
                    entity.setLastUsedAt(Instant.now());
                    passkeyCredentialRepo.save(entity);
                });
    }

    private UserEntity requireCurrentUser(String requestId) {
        UUID userId = getCurrentUserId(requestId);
        return userRepo.findById(userId)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "User not found",
                        HttpStatus.UNAUTHORIZED.value(), requestId));
    }

    private UUID getCurrentUserId(String requestId) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticationUserDetails details) {
            return details.getUserId();
        }
        throw new ApiException(ApiException.ErrorCode.UNAUTHORIZED, "Authentication required",
                HttpStatus.UNAUTHORIZED.value(), requestId);
    }

    private UserEntity resolveUserByIdentifier(String identifier, String requestId) {
        Optional<UserEntity> byEmail = userRepo.findOneByEmail(identifier);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }
        return userRepo.findOneByUsername(identifier)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "User not found",
                        HttpStatus.NOT_FOUND.value(), requestId));
    }

    private UserIdentity buildUserIdentity(UserEntity user) {
        String displayName = user.getFullName() != null ? user.getFullName()
                : (user.getFirstName() != null ? user.getFirstName() : user.getUsername());
        return UserIdentity.builder()
                .name(user.getUsername())
                .displayName(displayName)
                .id(PasskeyUserHandle.toByteArray(user.getId()))
                .build();
    }

    private String getChallengeOrThrow(String redisKey, String requestId) {
        Object stored = redisService.get(redisKey);
        if (stored == null) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "Passkey challenge expired or not found",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }
        return stored.toString();
    }

    private String registerChallengeKey(String username) {
        return RedisKey.PASSKEY_REGISTER_CHALLENGE_PREFIX + username;
    }

    private String loginChallengeKey(String sessionId) {
        return RedisKey.PASSKEY_LOGIN_CHALLENGE_PREFIX + sessionId;
    }

    private Duration challengeDuration() {
        return Duration.ofSeconds(webAuthnProperties.getChallengeTtlSeconds());
    }

    private PasskeyCredentialInfo toInfo(UserPasskeyCredentialEntity entity) {
        return PasskeyCredentialInfo.builder()
                .id(entity.getId())
                .deviceName(entity.getDeviceName())
                .transports(entity.getTransports())
                .discoverable(Boolean.TRUE.equals(entity.getIsDiscoverable()))
                .backupEligible(Boolean.TRUE.equals(entity.getIsBackupEligible()))
                .backedUp(Boolean.TRUE.equals(entity.getIsBackedUp()))
                .active(Boolean.TRUE.equals(entity.getIsActive()))
                .createdAt(entity.getCreatedAt())
                .lastUsedAt(entity.getLastUsedAt())
                .build();
    }

    private String formatTransports(java.util.Set<AuthenticatorTransport> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.stream()
                .map(AuthenticatorTransport::getId)
                .collect(Collectors.joining(","));
    }
}
