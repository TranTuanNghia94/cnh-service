package com.cnh.ies.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.yubico.webauthn.data.ByteArray;

class PasskeyUserHandleTest {

    @Test
    void fromUserId_isStableSixteenBytes() {
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        byte[] handle = PasskeyUserHandle.fromUserId(userId);

        assertEquals(16, handle.length);

        ByteBuffer expected = ByteBuffer.allocate(16);
        expected.putLong(userId.getMostSignificantBits());
        expected.putLong(userId.getLeastSignificantBits());
        assertArrayEquals(expected.array(), handle);
    }

    @Test
    void toByteArray_wrapsSameBytes() {
        UUID userId = UUID.randomUUID();
        ByteArray byteArray = PasskeyUserHandle.toByteArray(userId);
        assertArrayEquals(PasskeyUserHandle.fromUserId(userId), byteArray.getBytes());
    }
}
