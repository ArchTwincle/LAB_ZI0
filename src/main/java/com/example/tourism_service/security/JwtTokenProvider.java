package com.example.tourism_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // В реальном проекте вынеси это в application.properties
    private final String secret = "myVerySecretKeyForJwtAuthenticationMustBeVeryLongAndStrong123456";
    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    // Access-токен на 15 минут
    private final long accessExpiration = 15 * 60 * 1000;
    // Refresh-токен на 7 дней
    private final long refreshExpiration = 7 * 24 * 60 * 60 * 1000;

    public String generateAccessToken(String username) {
        return generateToken(username, accessExpiration);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, refreshExpiration);
    }

    private String generateToken(String username, long expiry) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false; // Токен невалиден или просрочен
        }
    }
}