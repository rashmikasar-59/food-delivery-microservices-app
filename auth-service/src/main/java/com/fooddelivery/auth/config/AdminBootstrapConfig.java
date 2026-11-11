package com.fooddelivery.auth.config;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    public CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword) {

        return args -> {
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                System.out.println(
                        "Admin bootstrap skipped: credentials not configured."
                );
                return;
            }

            if (userRepository.existsByRole(Role.ADMIN)) {
                System.out.println("Admin account already exists.");
                return;
            }

            String email = adminEmail.trim().toLowerCase();

            if (userRepository.existsByEmail(email)) {
                throw new IllegalStateException(
                        "Admin email belongs to an existing non-admin account. "
                        + "No role changes were made."
                );
            }

            User admin = new User(
                    "System Administrator",
                    email,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN
            );

            userRepository.save(admin);

            System.out.println("Initial admin account created.");
        };
    }
}
