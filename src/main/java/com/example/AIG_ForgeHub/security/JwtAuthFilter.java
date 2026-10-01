package com.example.AIG_ForgeHub.security;

import com.example.AIG_ForgeHub.serviceImp.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = getToken(request);

        if (token == null ||
                SecurityContextHolder
                        .getContext()
                        .getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        try {

            String email = jwtService.extractSubject(token);

            /*
             * Login ke baad jo temporary token banta hai
             * usme mfaVerified=false hota hai.
             *
             * Us token se dashboard access nahi denge.
             */
            if (!jwtService.isMfaVerified(token)) {

                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails =
                    customUserDetailsService
                            .loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContext securityContext =
                    SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(authenticationToken);

            SecurityContextHolder.setContext(securityContext);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header != null &&
                header.startsWith(BEARER_PREFIX)) {

            return header.substring(BEARER_PREFIX.length());
        }

        if (request.getCookies() != null) {

            for (Cookie cookie : request.getCookies()) {

                if ("accessToken".equals(cookie.getName())) {

                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}