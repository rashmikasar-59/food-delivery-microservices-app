
package com.fooddelivery.auth.event;

import java.time.LocalDateTime;

public class UserRegisteredEvent {

    private String userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime registeredAt;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(
            String userId,
            String name,
            String email,
            String role,
            LocalDateTime registeredAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.registeredAt = registeredAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
}
