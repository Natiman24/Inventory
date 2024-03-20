package com.example.mongo.user;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    public Optional<User> findByPhoneNumber(String phoneNumber);
    public Optional<User> findById(String id);

    public List<User> findByRole(String role);
}
