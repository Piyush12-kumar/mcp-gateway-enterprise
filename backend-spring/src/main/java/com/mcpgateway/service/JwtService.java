package com.mcpgateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Creates and validates signed JSON Web Tokens (HS256).
 * The token carries the identity (username/subject), the userId, the orgId,
 * and the user's roles — so downstream code knows WHO is calling without
 * trusting client-supplied headers.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret:change-me-in-prod-please-use-a-long-random-32B+-secret}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs) {
        // HS256 requires a key >= 32 bytes. We derive it from the configured secret.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Issue a token for a successfully authenticated user. */
    public String generateToken(String username, Long userId, Long orgId, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "userId", userId,
                        "orgId", orgId == null ? -1L : orgId,
                        "roles", roles))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /** Parse & verify a token, returning its claims. Throws if invalid/expired. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

