package com.example.readingrewards.auth.service;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        if (username.contains("@")) {
            user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Parent not found by email: " + username));
        } else {
            user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Child not found by username: " + username));
        }

        return org.springframework.security.core.userdetails.User.withUsername(username)
            .password(user.getPassword())
            .roles(user.getRole().name())
            .build();
    }
}