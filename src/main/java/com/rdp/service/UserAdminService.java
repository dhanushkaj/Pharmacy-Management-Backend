package com.rdp.service;

import com.rdp.model.Role;
import com.rdp.model.User;
import com.rdp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserAdminService {
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired(required = false) private JavaMailSender mailSender;

    public String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    @Transactional
    public Map<String, Object> createUser(String username, String email, Set<Role> roles) {
        String password = generateRandomPassword(10);
        User user = new User();
        // Ensure userId is not set so DB can auto-generate it
        user.setUserId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setRoles(roles);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        if (mailSender != null) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("Your Account Credentials");
            msg.setText("Username: " + username + "\nPassword: " + password);
            mailSender.send(msg);
        }
        Map<String, Object> result = new HashMap<>();
        // Avoid infinite recursion: only return user fields and role names
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("address", user.getAddress());
        userMap.put("phone", user.getPhone());
        userMap.put("roles", user.getRoles().stream().map(r -> r.getRoleName()).toList());
        result.put("user", userMap);
        result.put("password", password);
        return result;
    }
}
