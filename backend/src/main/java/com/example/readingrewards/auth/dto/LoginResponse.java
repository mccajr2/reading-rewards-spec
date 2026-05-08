package com.example.readingrewards.auth.dto;

public record LoginResponse(String token, AuthUserResponse user) {
}