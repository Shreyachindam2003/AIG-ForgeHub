package com.example.AIG_ForgeHub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX="Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain filterChain) throws ServletException,IOException {

        String token=getToken(request);

        if (token==null || SecurityContextHolder.getContext().getAuthentication()!=null) {
            filterChain.doFilter(request,response);
            return;
        }

        try {

            if (!jwtService.isTokenValid(token)) {
                log.warn("Invalid JWT token for request: {}",request.getRequestURI());
                filterChain.doFilter(request,response);
                return;
            }

            if (!jwtService.isAccessToken(token)) {
                log.warn("Non-access JWT token used for request: {}",request.getRequestURI());
                filterChain.doFilter(request,response);
                return;
            }

            if (!jwtService.isMfaVerified(token)) {
                log.warn("JWT rejected because MFA is not verified for request: {}",request.getRequestURI());
                filterChain.doFilter(request,response);
                return;
            }

            String email=jwtService.extractSubject(token);
            String role=jwtService.extractRole(token);

            if (email==null || role==null) {
                log.warn("JWT missing required claims for request: {}",request.getRequestURI());
                filterChain.doFilter(request,response);
                return;
            }

            UsernamePasswordAuthenticationToken authenticationToken=
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_"+role))
                    );

            SecurityContext securityContext=SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authenticationToken);
            SecurityContextHolder.setContext(securityContext);

            log.info("JWT authentication successful for user: {} with role: {}",email,role);

        } catch (Exception e) {

            log.error("JWT authentication failed for request: {}",request.getRequestURI(),e);

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request,response);
    }

    private String getToken(HttpServletRequest request) {

        String header=request.getHeader("Authorization");

        if (header!=null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}