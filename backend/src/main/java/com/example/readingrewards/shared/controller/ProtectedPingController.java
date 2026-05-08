package com.example.readingrewards.shared.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedPingController {

    @GetMapping("/protected-ping")
    public ResponseEntity<String> protectedPing() {
        return ResponseEntity.ok("pong");
    }
}