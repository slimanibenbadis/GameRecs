package com.gamerecs.back.controller;

import com.gamerecs.back.dto.LoginRequestDto;
import com.gamerecs.back.dto.LoginResponseDto;
import com.gamerecs.back.exception.TooManyRequestsException;
import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.service.JwtService;
import com.gamerecs.back.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final LoginAttemptService loginAttemptService;

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            CsrfTokenRepository csrfTokenRepository,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest, HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIP(request);
        logger.debug("Attempting login for user: {} from IP: {}", loginRequest.getUsername(), clientIp);

        if (loginAttemptService.isBlocked(clientIp)) {
            logger.warn("Login attempt blocked for IP: {} due to excessive failures.", clientIp);
            throw new TooManyRequestsException("You have exceeded the maximum number of login attempts. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found after successful authentication: {}", userDetails.getUsername());
                    return new IllegalStateException("User not found after authentication");
                });

            loginAttemptService.loginSucceeded(clientIp);
            logger.info("User successfully logged in: {} from IP: {}", user.getUsername(), clientIp);

            String token = jwtService.generateToken(userDetails);
            this.csrfTokenRepository.saveToken(this.csrfTokenRepository.generateToken(request), request, response);
            logger.debug("Explicitly saved CSRF token to response for user: {}", user.getUsername());

            LoginResponseDto responseDto = LoginResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .build();

            return ResponseEntity.ok(responseDto);

        } catch (AuthenticationException e) {
            logger.warn("Login failed for user: {} from IP: {}. Reason: {}", loginRequest.getUsername(), clientIp, e.getMessage());
            loginAttemptService.loginFailed(clientIp);
            throw e;
        }
    }

    @GetMapping("/oauth2/failure")
    public ResponseEntity<Map<String, Object>> handleOAuth2Failure() {
        logger.debug("Handling OAuth2 authentication failure");
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "OAuth2 authentication failed");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
} 
