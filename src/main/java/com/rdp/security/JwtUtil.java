package com.rdp.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final String jwtSecret = "yourSuperLongSecretKeyThatIsAtLeast64CharactersLongForHS512Algorithm1234567890";
    
    // Token expiration: 1 hour (3600 seconds)
    private final long jwtExpirationMs = 1 * 60 * 60 * 1000; // 1 hour
    // Warning time: 1 minute before expiration
    private final long warningTimeMs = 1 * 60 * 1000; // 1 minute
    
    private final SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
    
    public long getWarningTimeMs() {
        return warningTimeMs;
    }
    
    public Date getTokenExpirationDate(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            return claims.getExpiration();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
    
    public long getTokenExpirationTime(String token) {
        Date expirationDate = getTokenExpirationDate(token);
        if (expirationDate != null) {
            return expirationDate.getTime();
        }
        return 0;
    }
}
