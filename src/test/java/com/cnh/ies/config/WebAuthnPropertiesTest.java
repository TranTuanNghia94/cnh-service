package com.cnh.ies.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WebAuthnPropertiesTest {

    @Test
    void getAllowedOrigins_splitsCommaSeparatedValues() {
        WebAuthnProperties properties = new WebAuthnProperties();
        properties.setOrigins("http://localhost:3000, http://localhost:4200");

        assertEquals(2, properties.getAllowedOrigins().size());
        assertTrue(properties.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(properties.getAllowedOrigins().contains("http://localhost:4200"));
    }
}
