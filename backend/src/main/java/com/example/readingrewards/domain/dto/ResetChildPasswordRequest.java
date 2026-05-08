package com.example.readingrewards.domain.dto;

public class ResetChildPasswordRequest {
    private String childUsername;
    private String newPassword;

    public String getChildUsername() { return childUsername; }
    public void setChildUsername(String childUsername) { this.childUsername = childUsername; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
