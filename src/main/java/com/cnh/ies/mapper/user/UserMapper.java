package com.cnh.ies.mapper.user;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.model.user.UserInfo;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public UserInfo mapToUserInfo(UserEntity user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setFullName(user.getFullName());
        userInfo.setEmail(user.getEmail());
        userInfo.setIsActive(user.getIsActive());
        return userInfo;
    }
}
