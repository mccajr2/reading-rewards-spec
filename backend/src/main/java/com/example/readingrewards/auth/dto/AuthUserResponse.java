package com.example.readingrewards.auth.dto;

import com.example.readingrewards.auth.model.User;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    User.UserRole role,
    UUID parentId,
    String email,
    String username,
    String firstName,
    String status
) {
}