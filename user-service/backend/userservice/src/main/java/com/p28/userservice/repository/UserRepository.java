package com.p28.userservice.repository;

import java.util.Optional;

import com.p28.userservice.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserId(String userId);

    Optional<User> findByFirebaseUid(String firebaseUid);
}