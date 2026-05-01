package com.rdp.controller;

import com.rdp.model.User;
import com.rdp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Get current user profile
    @GetMapping("/me")
    public User getSelf(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    // Self-update endpoint (address, phone, email, password)
    @PutMapping("/me")
    public User updateSelf(@RequestBody Map<String, String> payload, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (payload.containsKey("email")) user.setEmail(payload.get("email"));
        if (payload.containsKey("address")) user.setAddress(payload.get("address"));
        if (payload.containsKey("phone")) user.setPhone(payload.get("phone"));
        if (payload.containsKey("password") && payload.get("password") != null && !payload.get("password").isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(payload.get("password")));
        }
        return userRepository.save(user);
    }
}
