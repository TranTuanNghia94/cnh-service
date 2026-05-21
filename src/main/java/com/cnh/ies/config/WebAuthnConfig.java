package com.cnh.ies.config;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cnh.ies.service.auth.PasskeyCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebAuthnConfig {

    private final WebAuthnProperties webAuthnProperties;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Bean
    public RelyingParty relyingParty() {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(webAuthnProperties.getRpId())
                .name(webAuthnProperties.getRpName())
                .build();

        Set<String> origins = webAuthnProperties.getAllowedOrigins();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(passkeyCredentialRepository)
                .origins(origins)
                .build();
    }
}
