package com.rdp.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.User;
import com.rdp.repository.UserRepository;
import com.rdp.service.UserAdminService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserAdminService userAdminService;

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

    // Get current user's session code
    @GetMapping("/me/session-code")
    public Map<String, String> getSessionCode(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        return Map.of("sessionCode", user.getSessionCode() != null ? user.getSessionCode() : "");
    }

    // Generate a new session code for current user
    @PostMapping("/me/regenerate-session-code")
    public Map<String, String> regenerateSessionCode(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        String newCode = userAdminService.generateAndSaveSessionCode(user.getUserId());
        return Map.of("sessionCode", newCode, "message", "Session code regenerated successfully");
    }
}
