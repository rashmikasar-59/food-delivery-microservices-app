
package com.fooddelivery.user.security;

import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.repository.UserProfileRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserProfileRepository repository;

    public CustomUserDetailsService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        UserProfile profile = repository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email));

        return new User(
                profile.getEmail(),
                "",
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + profile.getRole()
                        )
                )
        );
    }
}
