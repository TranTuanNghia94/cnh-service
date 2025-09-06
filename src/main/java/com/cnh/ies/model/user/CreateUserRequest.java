package com.cnh.ies.model.user;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;

    private String password;

    private String email;

    private String firstName;

    private String lastName;    

    private String fullName;
    
    private String phone;

    private String role;
}
