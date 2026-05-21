package com.cnh.ies.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.mapper.user.UserMapper;
import com.cnh.ies.model.user.ChangePasswordRequest;
import com.cnh.ies.model.user.UpdateSelfProfileRequest;
import com.cnh.ies.model.user.UserInfo;
import com.cnh.ies.repository.auth.RoleRepo;
import com.cnh.ies.repository.auth.UserRepo;

@ExtendWith(MockitoExtension.class)
class UserServiceSelfTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepo roleRepo;

    @InjectMocks
    private UserService userService;

    private UserEntity userEntity;

    @BeforeEach
    void setSecurityContext() {
        userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID());
        userEntity.setUsername("alice");
        userEntity.setPassword(BCrypt.hashpw("oldpass", BCrypt.gensalt()));
        userEntity.setIsActive(true);
        userEntity.setFirstName("Alice");
        userEntity.setLastName("Smith");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMyProfile_updatesOnlyProfileFields() {
        when(userRepo.findOneByUsername("alice")).thenReturn(Optional.of(userEntity));
        when(userMapper.mapToUserInfo(userEntity)).thenReturn(new UserInfo());

        UpdateSelfProfileRequest request = UpdateSelfProfileRequest.builder()
                .firstName("Alicia")
                .lastName("Smith")
                .phone("+84123456789")
                .build();

        userService.updateMyProfile(request, "req-1");

        assertEquals("Alicia", userEntity.getFirstName());
        assertEquals("Smith", userEntity.getLastName());
        assertEquals("+84123456789", userEntity.getPhone());
        verify(userRepo).save(userEntity);
    }

    @Test
    void changeMyPassword_requiresCorrectOldPassword() {
        when(userRepo.findOneByUsername("alice")).thenReturn(Optional.of(userEntity));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrong")
                .newPassword("newpass")
                .build();

        assertThrows(ApiException.class, () -> userService.changeMyPassword(request, "req-1"));
    }

    @Test
    void changeMyPassword_updatesPasswordWhenOldMatches() {
        when(userRepo.findOneByUsername("alice")).thenReturn(Optional.of(userEntity));

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldpass")
                .newPassword("newpass")
                .build();

        String result = userService.changeMyPassword(request, "req-1");

        assertEquals("Password changed successfully", result);
        verify(userRepo).save(any(UserEntity.class));
    }
}
