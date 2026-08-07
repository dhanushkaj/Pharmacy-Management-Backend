package com.rdp.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.User;
import com.rdp.repository.UserRepository;
import com.rdp.security.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    public static final String JWT_COOKIE_NAME = "jwt_token";

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String username = payload.get("username");
        String password = payload.get("password");
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        List<String> roles = user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toList());
        String token = jwtUtil.generateToken(username, roles);
        
        // Set JWT in HTTP-only cookie
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, token)
            .httpOnly(true)                    // Cannot be accessed by JavaScript
            .secure(false)                     // Set to true in production (HTTPS only)
            .path("/")                         // Available for all paths
            .maxAge(jwtUtil.getJwtExpirationMs() / 1000)  // 1 hour in seconds
            .sameSite("Lax")                   // CSRF protection
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        // Return user info (but NOT the token - it's in the cookie)
        return ResponseEntity.ok(Map.of(
            "username", username,
            "roles", roles,
            "message", "Login successful"
        ));
    }

    @PostMapping("/switch-user")
    public ResponseEntity<Map<String, Object>> switchUserByCode(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String sessionCode = payload.get("sessionCode");
        if (sessionCode == null || sessionCode.isBlank()) {
            throw new RuntimeException("Session code is required");
        }
        
        User user = userRepository.findBySessionCode(sessionCode).orElse(null);
        if (user == null) {
            throw new RuntimeException("Invalid session code");
        }
        
        List<String> roles = user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toList());
        String token = jwtUtil.generateToken(user.getUsername(), roles);
        
        // Set JWT in HTTP-only cookie
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(jwtUtil.getJwtExpirationMs() / 1000)
            .sameSite("Lax")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        // Return user info
        return ResponseEntity.ok(Map.of(
            "username", user.getUsername(),
            "roles", roles,
            "message", "User session switched successfully"
        ));
    }
    
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(HttpServletRequest request) {
        // Return token expiration info for session timeout monitoring
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateJwtToken(token)) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid or expired token"
            ));
        }
        
        String username = jwtUtil.getUsernameFromToken(token);
        long expirationTime = jwtUtil.getTokenExpirationTime(token);
        long currentTime = System.currentTimeMillis();
        long timeRemaining = expirationTime - currentTime;
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "username", username,
            "expirationTime", expirationTime,
            "currentTime", currentTime,
            "timeRemaining", timeRemaining,
            "expirationDurationMs", jwtUtil.getJwtExpirationMs(),
            "warningTimeMs", jwtUtil.getWarningTimeMs()
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear the JWT cookie by setting it to empty with immediate expiration
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(false)                     // Set to true in production
            .path("/")
            .maxAge(0)                         // Immediate expiration
            .sameSite("Lax")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        // This endpoint validates the current token from cookie
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateJwtToken(token)) {
            return ResponseEntity.status(401).body(Map.of(
                "valid", false,
                "message", "Invalid or expired token"
            ));
        }
        
        String username = jwtUtil.getUsernameFromToken(token);
        List<String> roles = jwtUtil.getRolesFromToken(token);
        
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "username", username,
            "roles", roles
        ));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Refresh the token if the current one is still valid
        String token = extractTokenFromCookie(request);
        if (token == null || !jwtUtil.validateJwtToken(token)) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid or expired token"
            ));
        }
        
        String username = jwtUtil.getUsernameFromToken(token);
        List<String> roles = jwtUtil.getRolesFromToken(token);
        
        // Generate new token
        String newToken = jwtUtil.generateToken(username, roles);
        
        // Set new JWT cookie
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, newToken)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(jwtUtil.getJwtExpirationMs() / 1000)
            .sameSite("Lax")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Token refreshed"
        ));
    }
    
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
