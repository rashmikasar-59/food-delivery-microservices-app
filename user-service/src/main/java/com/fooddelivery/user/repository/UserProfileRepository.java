package com.fooddelivery.user.repository;

import com.fooddelivery.user.entity.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserProfileRepository
        extends MongoRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);
}