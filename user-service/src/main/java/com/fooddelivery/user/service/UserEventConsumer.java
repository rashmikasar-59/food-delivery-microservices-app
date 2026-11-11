package com.fooddelivery.user.service;

import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.event.UserRegisteredEvent;
import com.fooddelivery.user.repository.UserProfileRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserEventConsumer {

    private final UserProfileRepository userProfileRepository;

    public UserEventConsumer(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(
        topics = "user.registered",
        groupId = "user-service-group"
    )
    public void consumeUserRegisteredEvent(UserRegisteredEvent event) {

        if (userProfileRepository.existsByUserId(event.getUserId())) {
            System.out.println("User profile already exists: " + event.getUserId());
            return;
        }

        UserProfile profile = new UserProfile(
            event.getUserId(),
            event.getName(),
            event.getEmail(),
            event.getRole(),
            event.getRegisteredAt()
        );

        userProfileRepository.save(profile);

        System.out.println(
            "User profile created successfully: " + event.getEmail()
        );
    }
}
