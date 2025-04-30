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
        filterLogger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        final String jwt = extractJwtFromRequest(request);

        if (jwt == null) {
            filterLogger.debug("No JWT found in request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        filterLogger.debug("Extracted JWT: {}", jwt); // Log JWT *after* checking it's not null

        String username = null;
        try {
            username = jwtService.extractUsername(jwt);
            filterLogger.debug("Extracted username from JWT: {}", username);
        } catch (Exception e) {
            filterLogger.warn("Failed to extract username from JWT: {}", e.getMessage());
            // If username extraction fails, no point proceeding.
            filterChain.doFilter(request, response);
            return;
        }

        // Only attempt authentication if username is extracted and no authentication exists yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean proceed = authenticateUserIfTokenValid(request, response, filterChain, jwt, username);
            if (!proceed) {
                // Error response was sent by authenticateUserIfTokenValid (CSRF failure)
                return; // Stop filter chain processing immediately
            }
        } else {
            filterLogger.debug("Username is null or SecurityContext already has Authentication. Username: {}, Authentication: {}",
                             username, SecurityContextHolder.getContext().getAuthentication());
        }

        // If we reach here, auth wasn't needed, or it was attempted and didn't require halting.
        filterChain.doFilter(request, response);
        filterLogger.debug("Finished processing request: {} {}", request.getMethod(), request.getRequestURI());
    }

    /**
     * Extracts the JWT token from the Authorization header.
     * @param request The incoming request.
     * @return The JWT token string or null if not found or not a Bearer token.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Attempts to load UserDetails, validate the JWT and CSRF token, and set authentication.
     * If CSRF fails, sends an error response and returns.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @param filterChain The filter chain.
     * @param jwt The JWT token.
     * @param username The username extracted from the JWT.
     * @return true if authentication should proceed, false if an error response was sent (CSRF failure)
     * @throws IOException If sending an error response fails.
     * @throws ServletException If filtering fails.
     */
    private boolean authenticateUserIfTokenValid(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String jwt, String username)
            throws IOException, ServletException {
        filterLogger.debug("Attempting to load UserDetails for username: {}", username);
        UserDetails userDetails = loadUserDetails(username);

        if (userDetails == null) {
            // User not found or error loading, already logged, continue chain without authenticating
             filterLogger.debug("UserDetails not loaded for {}, continuing filter chain without authentication.", username);
            return true; // Indicate chain should continue
        }

        filterLogger.debug("Validating token for user: {}", username);
        if (jwtService.isTokenValid(jwt, userDetails)) {
            filterLogger.debug("JWT token is valid for user: {}", username);

            // Perform CSRF check; this method handles sending error response if fails
            if (!validateCsrfToken(request, response, username)) {
                return false; // Indicate chain should HALT
            }

            // Set authentication in security context
            setAuthentication(request, userDetails);
            filterLogger.info("Successfully authenticated user: {}", username);
        } else {
            filterLogger.warn("JWT token validation failed for user: {}", username);
        }
        return true; // Indicate chain should continue
    }

     /**
      * Loads UserDetails for the given username.
      * Handles and logs exceptions during loading.
      * @param username The username.
      * @return UserDetails or null if not found or an error occurred.
      */
     private UserDetails loadUserDetails(String username) {
         try {
             UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
             filterLogger.debug("Loaded UserDetails: {}", userDetails.getUsername());
             return userDetails;
         } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
             filterLogger.warn("User not found during authentication: {}", username);
         } catch (Exception e) {
             filterLogger.error("Error loading UserDetails for {}: {}", username, e.getMessage(), e);
         }
         return null; // Return null if user not found or error occurs
     }

    /**
     * Validates the CSRF token for methods requiring protection.
     * Sends an error response if validation fails.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @param username The username for logging purposes.
     * @return true if CSRF check passes or is not required, false if it fails.
     * @throws IOException If sending an error response fails.
     */
    private boolean validateCsrfToken(HttpServletRequest request, HttpServletResponse response, String username) throws IOException {
        if (METHODS_TO_CHECK.contains(request.getMethod())) {
            CsrfToken expectedToken = csrfTokenRepository.loadToken(request);
            String actualToken = request.getHeader("X-XSRF-TOKEN");

            if (expectedToken == null) {
                filterLogger.warn("CSRF check failed: Expected token not found in repository for user {}", username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing CSRF token");
                return false; // Indicate failure
            }
            if (actualToken == null || !actualToken.equals(expectedToken.getToken())) {
                filterLogger.warn("CSRF check failed: Invalid token received (Expected: '{}', Actual: '{}') for user {}",
                                 expectedToken.getToken(), actualToken, username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false; // Indicate failure
            }
            filterLogger.debug("CSRF check passed for user {}", username);
        }
        return true; // Indicate success or check not needed
    }

    /**
     * Sets the authentication token in the SecurityContextHolder.
     * @param request The incoming request.
     * @param userDetails The authenticated user details.
     */
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
} 
