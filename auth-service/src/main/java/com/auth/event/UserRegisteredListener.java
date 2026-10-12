package com.auth.event;

import com.auth.model.UserCredential;
import com.auth.model.UserRole;
import com.auth.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Idempotently mirrors the canonical User Service record. Registration and role changes
 * use the same topic and email key so events for one account retain partition order. */
@Component
public class UserRegisteredListener {
    private final UserRepository repository;
    public UserRegisteredListener(UserRepository repository) { this.repository = repository; }

    @KafkaListener(topics = "${app.kafka.user-registered-topic:user-registered-topic}",
                   groupId = "${spring.kafka.consumer.group-id:auth-user-sync}")
    public void onUserChanged(UserRegisteredEvent event) {
        if (event == null || event.getEmail() == null || event.getEmail().isBlank()
                || event.getPassword() == null || event.getPassword().isBlank() || event.getId() == null) {
            throw new IllegalArgumentException("Invalid user sync event: id, email and encoded password are required");
        }
        String email = event.getEmail().trim().toLowerCase(Locale.ROOT);
        UserCredential user = repository.findById(event.getId())
                .or(() -> repository.findByUsername(email))
                .orElseGet(UserCredential::new);
        user.setId(event.getId());
        user.setFullName(event.getFullName());
        user.setUsername(email);
        // The password was BCrypt-encoded by User Service. Never encode it again.
        user.setPassword(event.getPassword());
        user.setPhoneNumber(event.getPhoneNumber());
        UserRole role;
        try { role = event.getUserRole() == null ? UserRole.CUSTOMER
                : UserRole.valueOf(event.getUserRole().trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown role in user sync event", ex); }
        user.setUserRole(role);
        repository.save(user);
    }
}
