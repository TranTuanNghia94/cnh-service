package com.cnh.ies.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.redis.RedisService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAccessToken_includesUserNameClaims() {
        JwtService jwtService = new JwtService(mock(RedisService.class));
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(UUID.randomUUID());
        userInfo.setUsername("alice");
        userInfo.setFirstName("Alice");
        userInfo.setLastName("Nguyen");
        userInfo.setFullName("Nguyen Alice");

        String token = jwtService.generateAccessToken(userInfo);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("Alice", claims.get("firstName"));
        assertEquals("Nguyen", claims.get("lastName"));
        assertEquals("Nguyen Alice", claims.get("fullName"));
        assertFalse(claims.containsKey("permissions"));
    }
}
