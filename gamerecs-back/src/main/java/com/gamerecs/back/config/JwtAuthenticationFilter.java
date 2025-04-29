package com.gamerecs.back.config;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gamerecs.back.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter for validating JWT tokens in incoming requests.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger filterLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CsrfTokenRepository csrfTokenRepository;

    // HTTP methods that require CSRF protection
    private static final Set<String> METHODS_TO_CHECK = Set.of(HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.PATCH.name());

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, CsrfTokenRepository csrfTokenRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        filterLogger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterLogger.debug("No JWT Bearer token found in Authorization header for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        filterLogger.debug("Extracted JWT: {}", jwt);

        try {
            username = jwtService.extractUsername(jwt);
            filterLogger.debug("Extracted username from JWT: {}", username);
        } catch (Exception e) {
            filterLogger.warn("Failed to extract username from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails = null;
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            filterLogger.debug("Attempting to load UserDetails for username: {}", username);
            try {
                userDetails = this.userDetailsService.loadUserByUsername(username);
                filterLogger.debug("Loaded UserDetails: {}", userDetails.getUsername());
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                filterLogger.warn("User not found during authentication: {}", username);
                // Do not proceed with authentication, but continue filter chain
            } catch (Exception e) {
                filterLogger.error("Error loading UserDetails for {}: {}", username, e.getMessage(), e);
                // Potentially send error response or just continue chain depending on policy
            }

            // Only proceed if userDetails were successfully loaded
            if (userDetails != null) {
                filterLogger.debug("Validating token for user: {}", username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    filterLogger.debug("JWT token is valid for user: {}", username);

                    // --- Manual CSRF Check START ---
                    if (METHODS_TO_CHECK.contains(request.getMethod())) {
                        CsrfToken expectedToken = csrfTokenRepository.loadToken(request);
                        String actualToken = request.getHeader("X-XSRF-TOKEN");

                        if (expectedToken == null) {
                            filterLogger.warn("CSRF check failed: Expected token not found in repository for user {}", username);
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing CSRF token");
                            return; // Stop filter chain
                        }
                        if (actualToken == null || !actualToken.equals(expectedToken.getToken())) {
                            filterLogger.warn("CSRF check failed: Invalid token received (Expected: '{}', Actual: '{}') for user {}", 
                                             expectedToken.getToken(), actualToken, username);
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                            return; // Stop filter chain
                        }
                        filterLogger.debug("CSRF check passed for user {}", username);
                    }
                    // --- Manual CSRF Check END ---

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterLogger.info("Successfully authenticated user: {}", username);
                } else {
                    filterLogger.warn("JWT token validation failed for user: {}", username);
                }
            } else {
                filterLogger.debug("Username is null or SecurityContext already has Authentication. Username: {}, Authentication: {}", 
                                 username, SecurityContextHolder.getContext().getAuthentication());
            }
        } else {
             filterLogger.debug("Username is null or SecurityContext already has Authentication. Username: {}, Authentication: {}", 
                              username, SecurityContextHolder.getContext().getAuthentication());
        }
        
        filterChain.doFilter(request, response);
        filterLogger.debug("Finished processing request: {} {}", request.getMethod(), request.getRequestURI());
    }
} 
