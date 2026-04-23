package com.cnh.ies.service.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cnh.ies.constant.RedisKey;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.redis.RedisService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshExpiration;

    private final RedisService redisService;

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generate(Map<String, Object> extraClaims, String username) { 
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public String generateAccessToken(UserInfo userInfo) {
        log.debug("Generating access token for user '{}'", userInfo.getUsername());

        List<String> roles = userInfo.getRoles() == null ? List.of()
                : userInfo.getRoles().stream()
                        .map(r -> r.getCode())
                        .sorted()
                        .collect(Collectors.toList());

        Set<String> permissions = userInfo.getRoles() == null ? Set.of()
                : userInfo.getRoles().stream()
                        .filter(r -> r.getPermissions() != null)
                        .flatMap(r -> r.getPermissions().stream())
                        .map(p -> p.getCode())
                        .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userInfo.getId() != null ? userInfo.getId().toString() : null);
        claims.put("username", userInfo.getUsername());
        claims.put("fullName", userInfo.getFullName());
        claims.put("email", userInfo.getEmail());
        claims.put("roles", roles);
        claims.put("permissions", permissions);

        log.debug("JWT claims — roles: {}, permissions: {}", roles, permissions);
        return generate(claims, userInfo.getUsername());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token, String username) {
        final String tokenUsername = extractUsername(token);

        String redisToken = (String) redisService.get(RedisKey.ACCESS_TOKEN_PREFIX + username);
        if (redisToken == null) {
            log.warn("Token validation failed: no active session found for user '{}'", username);
            return false;
        }
        if (!redisToken.equals(token)) {
            log.warn("Token validation failed: token mismatch for user '{}' (possible reuse of revoked token)", username);
            return false;
        }

        return tokenUsername.equals(username);
    }
}
