package com.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.userservice.model.Address;
import com.userservice.model.UserRole;

import lombok.Builder;
import lombok.Data;



@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String fullName;
    private String email;
    private Long phoneNumber;
    private Address address;
    
  
    private UserRole userRole;
    
}
