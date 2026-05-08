package com.example.readingrewards.auth.repo;

import com.example.readingrewards.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByVerificationToken(String verificationToken);

    List<User> findByParentId(java.util.UUID parentId);
}