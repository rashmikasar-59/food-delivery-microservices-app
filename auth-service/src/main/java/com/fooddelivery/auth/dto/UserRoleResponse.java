package com.fooddelivery.auth.dto;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import java.time.LocalDateTime;

public class UserRoleResponse {

    private final String id;
    private final String name;
    private final String email;
    private final Role role;
    private final LocalDateTime createdAt;

    public UserRoleResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
