package com.cnh.ies.util;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.yubico.webauthn.data.ByteArray;

public final class PasskeyUserHandle {

    private PasskeyUserHandle() {}

    public static byte[] fromUserId(UUID userId) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());
        return buffer.array();
    }

    public static ByteArray toByteArray(UUID userId) {
        return new ByteArray(fromUserId(userId));
    }
}
