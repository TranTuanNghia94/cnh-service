package com.cnh.ies.service.auth;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cnh.ies.entity.auth.UserPasskeyCredentialEntity;
import com.cnh.ies.repository.auth.UserPasskeyCredentialRepo;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasskeyCredentialRepository implements CredentialRepository {

    private final UserPasskeyCredentialRepo passkeyCredentialRepo;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return passkeyCredentialRepo.findActiveByUsername(username).stream()
                .map(this::toDescriptor)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        List<UserPasskeyCredentialEntity> credentials = passkeyCredentialRepo.findActiveByUsername(username);
        if (credentials.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ByteArray(credentials.get(0).getUserHandle()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        List<UserPasskeyCredentialEntity> credentials = passkeyCredentialRepo.findActiveByUserHandle(userHandle.getBytes());
        if (credentials.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentials.get(0).getUser().getUsername());
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return passkeyCredentialRepo.findActiveByCredentialId(credentialId.getBytes())
                .filter(c -> java.util.Arrays.equals(c.getUserHandle(), userHandle.getBytes()))
                .map(this::toRegisteredCredential);
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        Optional<UserPasskeyCredentialEntity> entity = passkeyCredentialRepo.findActiveByCredentialId(credentialId.getBytes());
        if (entity.isEmpty()) {
            return Set.of();
        }
        return Set.of(toRegisteredCredential(entity.get()));
    }

    private RegisteredCredential toRegisteredCredential(UserPasskeyCredentialEntity entity) {
        return RegisteredCredential.builder()
                .credentialId(new ByteArray(entity.getCredentialId()))
                .userHandle(new ByteArray(entity.getUserHandle()))
                .publicKeyCose(new ByteArray(entity.getPublicKeyCose()))
                .signatureCount(entity.getSignatureCount())
                .build();
    }

    private PublicKeyCredentialDescriptor toDescriptor(UserPasskeyCredentialEntity entity) {
        PublicKeyCredentialDescriptor.PublicKeyCredentialDescriptorBuilder builder = PublicKeyCredentialDescriptor.builder()
                .id(new ByteArray(entity.getCredentialId()));
        if (entity.getTransports() != null && !entity.getTransports().isBlank()) {
            builder.transports(parseTransports(entity.getTransports()));
        }
        return builder.build();
    }

    private Set<com.yubico.webauthn.data.AuthenticatorTransport> parseTransports(String transports) {
        return java.util.Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(com.yubico.webauthn.data.AuthenticatorTransport::of)
                .collect(Collectors.toSet());
    }
}
