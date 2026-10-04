package com.example.AIG_ForgeHub.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN="accessToken";
    private static final String REFRESH_TOKEN="refreshToken";

    @Value("${app.cookie-secure:false}")
    private boolean secure;

    public void addAccessTokenCookie(HttpServletResponse response,String token,long expirationMs) {

        ResponseCookie cookie=ResponseCookie.from(ACCESS_TOKEN,token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public void addRefreshTokenCookie(HttpServletResponse response,String token,long expirationMs) {

        ResponseCookie cookie=ResponseCookie.from(REFRESH_TOKEN,token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {

        ResponseCookie cookie=ResponseCookie.from(ACCESS_TOKEN,"")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {

        ResponseCookie cookie=ResponseCookie.from(REFRESH_TOKEN,"")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }
}