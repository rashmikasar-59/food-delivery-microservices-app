package com.userservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.userservice.model.User;
import com.userservice.model.UserRole;



public interface UserRepository extends MongoRepository<User, String> {
	 List<User> findByUserRole(UserRole userRole);
    Optional<User> findByEmail(String email);
}
