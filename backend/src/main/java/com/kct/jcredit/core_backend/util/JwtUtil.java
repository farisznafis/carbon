package com.kct.jcredit.core_backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    // 🔒 Use a strong key (at least 32 bytes)
    private static final String SECRET = "my_super_secret_key_for_jwt_signing_12345";
    private static final long EXPIRATION = 1000 * 60 * 60; // 1 hour

    private SecretKey getSigningKey() {
        // Using raw bytes safely (no Base64 decoding)
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // Generate token with username, companyId, and role
    public String generateToken(Integer userId, String username, String companyId, String role) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .addClaims(Map.of(
                "userId", userId,
                "companyId", companyId,
                "role", role
            ))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())  // use SecretKey, not String
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    // Extract username (subject)
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract company ID
    public String extractCompanyId(String token) {
        return extractAllClaims(token).get("companyId", String.class);
    }

    // Extract role
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // Extract user ID
    public Integer extractUserId(String token) {
        return extractAllClaims(token).get("userId", Integer.class);
    }

    // Validate token (optional helper)
    public boolean validateToken(String token, String username) {
        String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    // Extract expiration as LocalDateTime
    public LocalDateTime extractExpirationAsLocalDateTime(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception e) {
            System.err.println("Error extracting expiration: " + e.getMessage());
            // エラー時は24時間後を返す
            return LocalDateTime.now().plusHours(24);
        }
    }
    
    public long extractIssuedAtMillis(String token) {
        Date issuedAt = extractAllClaims(token).getIssuedAt();
        return issuedAt != null ? issuedAt.getTime() : 0L;
    }
}
