package com.example.readingrewards.auth.dto;

public record SignupRequest(String email, String password, String firstName, String lastName) {
}