

package com.rdp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.Role;
import com.rdp.model.User;
import com.rdp.repository.RoleRepository;
import com.rdp.repository.UserRepository;
import com.rdp.service.UserAdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    @Autowired private UserAdminService userAdminService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    // Admin: create user
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        String email = (String) payload.get("email");
        Object rolesObj = payload.get("roles");
        List<String> roleNames = new ArrayList<>();
        if (rolesObj instanceof List<?>) {
            for (Object o : (List<?>) rolesObj) {
                if (o != null) roleNames.add(o.toString());
            }
        }
        Set<Role> roles = roleRepository.findByRoleNameIn(roleNames);
            return userAdminService.createUser(username, email, roles);
        }

        // Admin: list all users (for grid)
        @GetMapping("/users")
        public List<Map<String, Object>> listUsers() {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("username", user.getUsername());
                userMap.put("email", user.getEmail());
                userMap.put("address", user.getAddress());
                userMap.put("phone", user.getPhone());
                userMap.put("sessionCode", user.getSessionCode() != null ? user.getSessionCode() : "");
                userMap.put("roles", user.getRoles() == null ? List.of() : user.getRoles().stream().map(Role::getRoleName).toList());
                result.add(userMap);
            }
            return result;
    }

    // Admin: regenerate session code for a user
    @PostMapping("/users/{userId}/regenerate-session-code")
    public Map<String, String> regenerateUserSessionCode(@PathVariable Long userId) {
        String newCode = userAdminService.generateAndSaveSessionCode(userId);
        return Map.of("sessionCode", newCode, "message", "Session code regenerated successfully");
    }

    // Admin: get all role names
    @GetMapping("/roles")
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream().map(Role::getRoleName).toList();
    }
}
