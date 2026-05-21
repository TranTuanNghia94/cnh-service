package com.cnh.ies.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.auth.ResponseLoginModel;
import com.cnh.ies.model.auth.passkey.PasskeyAssertionOptionsResponse;
import com.cnh.ies.model.auth.passkey.PasskeyCredentialInfo;
import com.cnh.ies.model.auth.passkey.PasskeyLoginOptionsRequest;
import com.cnh.ies.model.auth.passkey.PasskeyLoginVerifyRequest;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationOptionsResponse;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationVerifyRequest;
import com.cnh.ies.model.auth.passkey.PasskeyRegistrationVerifyResponse;
import com.cnh.ies.service.auth.PasskeyService;
import com.cnh.ies.util.RequestContext;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth/passkeys")
@RequiredArgsConstructor
@Tag(name = "Passkeys", description = "WebAuthn passkey registration and login APIs")
public class PasskeyController {

    private final PasskeyService passkeyService;

    @PostMapping("/register/options")
    public ApiResponse<PasskeyRegistrationOptionsResponse> registerOptions() {
        String requestId = RequestContext.getRequestIdOrGenerate();
        return ApiResponse.success(passkeyService.startRegistration(requestId), "Passkey registration options created");
    }

    @PostMapping("/register/verify")
    public ApiResponse<PasskeyRegistrationVerifyResponse> registerVerify(
            @Valid @RequestBody PasskeyRegistrationVerifyRequest payload) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        return ApiResponse.success(passkeyService.finishRegistration(payload, requestId), "Passkey registered successfully");
    }

    @GetMapping
    public ApiResponse<List<PasskeyCredentialInfo>> listCredentials() {
        String requestId = RequestContext.getRequestIdOrGenerate();
        return ApiResponse.success(passkeyService.listCredentials(requestId), "Passkeys retrieved");
    }

    @DeleteMapping("/{credentialId}")
    public ApiResponse<Void> deleteCredential(@PathVariable UUID credentialId) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        passkeyService.deleteCredential(credentialId, requestId);
        return ApiResponse.success(null, "Passkey removed");
    }

    @PostMapping("/login/options")
    public ApiResponse<PasskeyAssertionOptionsResponse> loginOptions(
            @RequestBody(required = false) PasskeyLoginOptionsRequest payload) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        return ApiResponse.success(passkeyService.startLogin(payload, requestId), "Passkey login options created");
    }

    @PostMapping("/login/verify")
    public ApiResponse<ResponseLoginModel> loginVerify(@Valid @RequestBody PasskeyLoginVerifyRequest payload) {
        String requestId = RequestContext.getRequestIdOrGenerate();
        return ApiResponse.success(passkeyService.finishLogin(payload, requestId), "Passkey login success");
    }
}
