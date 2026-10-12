package com.auth.service;

import com.auth.model.UserCredential;
import com.auth.model.UserRole;
import com.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    public AdminBootstrap(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public void run(ApplicationArguments args) {
        String username = System.getenv("BOOTSTRAP_ADMIN_USERNAME");
        String password = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");
        // Bootstrap is opt-in. Existing accounts are never overwritten.
        if (username == null || username.isBlank() || password == null || password.isBlank()) return;
        String normalized = username.trim().toLowerCase(java.util.Locale.ROOT);
        if (repository.findByUsername(normalized).isPresent()) return;
        UserCredential admin = new UserCredential();
        admin.setFullName("Platform Administrator");
        admin.setUsername(normalized);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setUserRole(UserRole.ADMIN);
        repository.save(admin);
    }
}
