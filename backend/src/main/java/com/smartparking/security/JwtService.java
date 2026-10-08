package com.smartparking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String CLAIM_TOKEN_VERSION = "ver";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String DEFAULT_DEV_SECRET = "smart-parking-development-secret-key-must-be-overridden";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("jwt secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        if (DEFAULT_DEV_SECRET.equals(properties.secret())) {
            log.warn("JWT secret is the built-in development default; set the JWT_SECRET environment variable before deploying");
        }
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, String role, long tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.expiration());
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username, long tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.refreshExpiration());
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
