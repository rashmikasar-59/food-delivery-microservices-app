package com.auth.config;

import com.auth.model.UserCredential;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private String username, password;
    private com.auth.model.UserRole role;

    public CustomUserDetails(UserCredential userCredential) {
        this.username = userCredential.getUsername();
        this.password = userCredential.getPassword();
        this.role = userCredential.getUserRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"));
        return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
