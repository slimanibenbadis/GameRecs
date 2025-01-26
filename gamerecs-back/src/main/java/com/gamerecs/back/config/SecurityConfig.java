package com.gamerecs.back.config;

import com.gamerecs.back.service.OAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${allowed.origins}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    private static final String[] SWAGGER_WHITELIST = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-ui/index.html",
        "/swagger-ui/swagger-ui.css",
        "/swagger-ui/swagger-ui-bundle.js",
        "/v3/api-docs/**",
        "/v3/api-docs/swagger-config",
        "/api-docs/**"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/users/register",
        "/api/users/verify",
        "/api/auth/login",
        "/login/oauth2/code/google",
        "/oauth2/authorization/google",
        "/oauth2/authorization/**",
        "/login/oauth2/code/**",
        "/api/auth/oauth2/failure",
        "/api/auth/google/callback"
    };

    private static final String[] TEST_ENDPOINTS = {
        "/api/test/**"
    };

    private static final String[] ACTUATOR_ENDPOINTS = {
        "/actuator",
        "/actuator/**",
        "/actuator/health",
        "/actuator/info",
        "/actuator/metrics"
    };

    @Autowired
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter, 
            UserDetailsService userDetailsService,
            OAuth2UserService oAuth2UserService,
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.debug("Configuring security with JWT, OAuth2, CORS, CSRF, Swagger UI whitelist, and public endpoints");
        
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(PUBLIC_ENDPOINTS)
                .ignoringRequestMatchers(SWAGGER_WHITELIST)
                .ignoringRequestMatchers(TEST_ENDPOINTS)
                .ignoringRequestMatchers(ACTUATOR_ENDPOINTS)
            )
            .oauth2Login(oauth2 -> {
                logger.debug("Configuring OAuth2 login");
                oauth2
                    .userInfoEndpoint(userInfo -> {
                        logger.debug("Configuring OAuth2 user info endpoint");
                        userInfo.userService(oAuth2UserService);
                    })
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureUrl("/api/auth/oauth2/failure")
                    .authorizationEndpoint(authorization -> {
                        logger.debug("Configuring OAuth2 authorization endpoint");
                        authorization.baseUri("/oauth2/authorization");
                    });
            })
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.debug("Unauthorized access attempt: {}", authException.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(TEST_ENDPOINTS).permitAll()
                .requestMatchers(ACTUATOR_ENDPOINTS).permitAll()
                .requestMatchers(req -> req.getMethod().equals("OPTIONS")).permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.debug("Creating CORS configuration source with allowed origins: {}", allowedOrigins);
        CorsConfiguration configuration = new CorsConfiguration();
        for (String origin : allowedOrigins.split(",")) {
            configuration.addAllowedOrigin(origin.trim());
        }
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 
