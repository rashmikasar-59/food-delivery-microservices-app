
package com.fooddelivery.auth.repository;

import com.fooddelivery.auth.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(com.fooddelivery.auth.entity.Role role);
}
