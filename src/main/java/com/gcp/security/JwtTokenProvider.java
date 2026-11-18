package com.gcp.security;

import com.gcp.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    public static final String COMPROMISED_DEFAULT_SECRET =
            "9a3f2c4e6b8a1d0f5e7c3b2a1f4e6d8c0b2a4e6f8a1c3e5d7b9a0c2e4f6a8b1d";

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("FATAL: JWT signing secret is missing. Set 'jwt.secret' or 'JWT_SECRET' environment variable.");
        }
        String cleanSecret = secret.trim();
        if (COMPROMISED_DEFAULT_SECRET.equalsIgnoreCase(cleanSecret)) {
            throw new IllegalStateException("FATAL: The publicly known default JWT secret is prohibited. Provide a secure, unique JWT secret.");
        }
        byte[] keyBytes = cleanSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("FATAL: JWT secret key must be at least 256 bits (32 bytes). Current key length: " + keyBytes.length + " bytes.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, Role role, Long companyId, String fullName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .claim("companyId", companyId)
                .claim("fullName", fullName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long getCompanyIdFromToken(String token) {
        Object cid = getClaims(token).get("companyId");
        if (cid instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
