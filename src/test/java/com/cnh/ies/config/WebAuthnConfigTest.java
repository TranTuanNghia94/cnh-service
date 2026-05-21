package com.cnh.ies.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.cnh.ies.service.auth.PasskeyCredentialRepository;
import com.yubico.webauthn.RelyingParty;

@SpringBootTest(classes = {WebAuthnConfig.class, WebAuthnProperties.class})
@ActiveProfiles("test")
class WebAuthnConfigTest {

    @MockBean
    private PasskeyCredentialRepository passkeyCredentialRepository;

    @Autowired
    private RelyingParty relyingParty;

    @Test
    void relyingPartyBeanIsCreated() {
        assertNotNull(relyingParty);
    }
}
