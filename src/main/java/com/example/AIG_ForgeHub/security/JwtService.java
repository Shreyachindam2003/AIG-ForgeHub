package com.example.AIG_ForgeHub.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final Long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") Long accessTokenExpirationMs) {

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );

        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(
            String email,
            String role,
            boolean mfaVerified) {

        Date now = new Date();

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("mfaVerified", mfaVerified)
                .issuedAt(now)
                .expiration(
                        new Date(
                                now.getTime() + accessTokenExpirationMs
                        )
                )
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(
            String email,
            String role,
            long expirationMs) {

        Date now = new Date();

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("type", "refresh")
                .claim("mfaVerified", true)
                .issuedAt(now)
                .expiration(
                        new Date(
                                now.getTime() + expirationMs
                        )
                )
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractSubject(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isMfaVerified(String token) {

        Boolean value = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("mfaVerified", Boolean.class);

        return Boolean.TRUE.equals(value);
    }

    public boolean isRefreshToken(String token) {

        String type = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);

        return "refresh".equals(type);
    }

    public boolean isTokenValid(String token) {

        try {

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}