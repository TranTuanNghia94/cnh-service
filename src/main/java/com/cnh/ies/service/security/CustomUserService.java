package com.cnh.ies.service.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.service.redis.RedisService;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserService implements UserDetailsService {

    private final RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.info("Loading user by username: {}", username);

            Object user = redisService.get(username);
            ObjectMapper objectMapper = new ObjectMapper();
            UserInfo userInfo = objectMapper.convertValue(user, UserInfo.class);

            if (userInfo == null) {
                throw new UsernameNotFoundException("User not found");
            }
            
            return User.withUsername(userInfo.getUsername()).password("").roles("USER").build();
        } catch (Exception e) {
            log.error("Error loading user by username: {} | \nError: {}", username, e.getMessage());
            throw new UsernameNotFoundException(e.getMessage());
        }
    }
    
}
