package com.gamerecs.back.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String[] SWAGGER_WHITELIST = {
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/swagger-ui/swagger-initializer.js",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml",
        "/api-docs",
        "/api-docs/**",
        "/api-docs.yaml",
        "/webjars/**",
        "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> {
                logger.info("Configuring authorization rules");
                auth
                    // Swagger UI and API docs
                    .requestMatchers(SWAGGER_WHITELIST).permitAll()
                    // Actuator endpoints
                    .requestMatchers("/actuator/**").permitAll()
                    // Health check
                    .requestMatchers("/health").permitAll()
                    // All other endpoints require authentication
                    .anyRequest().authenticated();
                logger.info("Authorization rules configured successfully");
            })
            .addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class);

        logger.info("Security filter chain configuration completed");
        return http.build();
    }

    private class RequestLoggingFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();
            boolean permitted = isPermittedPath(uri);
            logger.info("Processing request: {} {} (Permitted: {})", 
                httpRequest.getMethod(), 
                uri,
                permitted);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Request headers:");
                httpRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> 
                    logger.debug("{}: {}", headerName, httpRequest.getHeader(headerName))
                );
            }
            chain.doFilter(request, response);
        }

        private boolean isPermittedPath(String uri) {
            // First check exact matches
            for (String pattern : SWAGGER_WHITELIST) {
                if (!pattern.contains("**") && uri.equals(pattern)) {
                    logger.debug("Exact match found for URI: {} with pattern: {}", uri, pattern);
                    return true;
                }
            }
            
            // Then check pattern matches
            for (String pattern : SWAGGER_WHITELIST) {
                if (pattern.endsWith("/**")) {
                    String prefix = pattern.substring(0, pattern.length() - 2);
                    if (uri.startsWith(prefix)) {
                        logger.debug("Pattern match found for URI: {} with pattern: {}", uri, pattern);
                        return true;
                    }
                }
            }
            
            // Check actuator and health endpoints
            boolean isActuator = uri.startsWith("/actuator/");
            boolean isHealth = uri.equals("/health");
            if (isActuator || isHealth) {
                logger.debug("Actuator/Health match found for URI: {}", uri);
                return true;
            }
            
            logger.debug("No matches found for URI: {}", uri);
            return false;
        }
    }
} 
