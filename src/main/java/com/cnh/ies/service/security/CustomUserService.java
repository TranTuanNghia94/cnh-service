package com.cnh.ies.service.security;

import java.util.Map;

import org.springframework.stereotype.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.redis.RedisService;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserService implements UserDetailsService {

    private static final ObjectMapper USER_INFO_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);

            Object user = redisService.get(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }

            UserInfo userInfo = toUserInfo(user);
            if (userInfo == null || userInfo.getId() == null) {
                throw new UsernameNotFoundException("User not found");
            }

            return new AuthenticationUserDetails(userInfo);
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user by username: {} | Error: {}", username, e.getMessage(), e);
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private UserInfo toUserInfo(Object user) {
        if (user instanceof UserInfo info) {
            return info;
        }
        if (user instanceof Map<?, ?> map) {
            // Redis default-typing may wrap values; drop Jackson type metadata before mapping.
            Object cleaned = stripJacksonTypeMetadata(map);
            return USER_INFO_MAPPER.convertValue(cleaned, UserInfo.class);
        }
        return USER_INFO_MAPPER.convertValue(user, UserInfo.class);
    }

    @SuppressWarnings("unchecked")
    private Object stripJacksonTypeMetadata(Object value) {
        if (value instanceof Map<?, ?> map) {
            java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("@class".equals(key)) {
                    continue;
                }
                copy.put(key, stripJacksonTypeMetadata(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.ArrayList<Object> list = new java.util.ArrayList<>();
            for (Object item : iterable) {
                list.add(stripJacksonTypeMetadata(item));
            }
            return list;
        }
        return value;
    }
}
