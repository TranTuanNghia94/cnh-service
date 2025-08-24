package com.cnh.ies.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cnh.ies.service.security.CustomUserService;
import com.cnh.ies.service.security.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class JwtFilterConfig extends OncePerRequestFilter  {

    private final JwtService jwtService;
    private final CustomUserService customUserService;

    public JwtFilterConfig(JwtService jwtService, CustomUserService customUserService) {
        this.jwtService = jwtService;
        this.customUserService = customUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = getNormalizedRequestURI(request);
        
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromHeader(request);
        if (jwt == null) {
            sendUnauthorizedResponse(response, "No token provided");
            return;
        }

        String username = jwtService.extractUsername(jwt);
        if (!isValidToken(jwt, username)) {
            sendUnauthorizedResponse(response, "Invalid token");
            return;
        }

        if (jwtService.isTokenExpired(jwt)) {
            sendUnauthorizedResponse(response, "Token expired - user: " + username);
            return;
        }

        UserDetails userDetails = customUserService.loadUserByUsername(username);
        if (userDetails == null) {
            sendUnauthorizedResponse(response, "User not found - user: " + username);
            return;
        }

        setAuthentication(userDetails, request);
        filterChain.doFilter(request, response);
    }

    private String getNormalizedRequestURI(HttpServletRequest request) {
        return request.getRequestURI().replace("/api/v1", "");
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.equals("/auth/login") || 
               requestURI.startsWith("/actuator/") || 
               requestURI.startsWith("/v3/api-docs/") || 
               requestURI.startsWith("/swagger-ui/");
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private boolean isValidToken(String jwt, String username) {
        return username != null && jwtService.isTokenValid(jwt, username);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) {
        log.warn(message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
