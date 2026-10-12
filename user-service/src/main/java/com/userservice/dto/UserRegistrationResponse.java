package com.userservice.dto;

import com.userservice.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {
    private String id;
    private String fullName;
    private String email;
    private UserRole userRole;
    private String message;
}
