package com.cnh.ies.service.auth;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cnh.ies.repository.auth.UserRepo;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepo userRepo;
    

}
