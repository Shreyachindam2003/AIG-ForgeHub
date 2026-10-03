package com.example.AIG_ForgeHub.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final Long accessTokenExpirationMs;
    private final Long recoveryTokenExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,@Value("${jwt.access-token-expiration-ms}") Long accessTokenExpirationMs,@Value("${jwt.recovery-token-expiration-ms}") Long recoveryTokenExpirationMs) {

        this.secretKey=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs=accessTokenExpirationMs;
        this.recoveryTokenExpirationMs=recoveryTokenExpirationMs;

        log.info("JwtService initialized successfully");
    }

    public String generateAccessToken(String email,String role,boolean mfaVerified) {

        Date now=new Date();

        String token=Jwts.builder()
                .subject(email)
                .claim("role",role)
                .claim("type","access")
                .claim("mfaVerified",mfaVerified)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+accessTokenExpirationMs))
                .signWith(secretKey,Jwts.SIG.HS256)
                .compact();

        log.debug("Access token generated for user: {} with MFA status: {}",email,mfaVerified);

        return token;
    }

    public String generateRefreshToken(String email,String role,long expirationMs) {

        Date now=new Date();

        String token=Jwts.builder()
                .subject(email)
                .claim("role",role)
                .claim("type","refresh")
                .claim("mfaVerified",true)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+expirationMs))
                .signWith(secretKey,Jwts.SIG.HS256)
                .compact();

        log.debug("Refresh token generated for user: {}",email);

        return token;
    }

    public String generateRecoveryToken(String email,String role) {

        Date now=new Date();

        String token=Jwts.builder()
                .subject(email)
                .claim("role",role)
                .claim("type","recovery")
                .claim("mfaVerified",false)
                .issuedAt(now)
                .expiration(new Date(now.getTime()+recoveryTokenExpirationMs))
                .signWith(secretKey,Jwts.SIG.HS256)
                .compact();

        log.debug("Recovery token generated for user: {}",email);

        return token;
    }

    public String extractSubject(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractRole(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role",String.class);
    }

    public boolean isMfaVerified(String token) {

        Boolean value=Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("mfaVerified",Boolean.class);

        return Boolean.TRUE.equals(value);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public boolean isRecoveryToken(String token) {
        return "recovery".equals(getTokenType(token));
    }

    public boolean isTokenValid(String token) {

        try {

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            log.warn("JWT validation failed");

            return false;
        }
    }

    private String getTokenType(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type",String.class);
    }
}