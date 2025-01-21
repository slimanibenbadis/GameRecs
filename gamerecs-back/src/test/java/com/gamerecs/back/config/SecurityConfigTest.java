package com.gamerecs.back.config;

import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfigTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow access to Swagger UI endpoints without authentication")
    void shouldAllowSwaggerUIAccess() throws Exception {
        logger.debug("Testing access to Swagger UI endpoints");
        
        String[] swaggerEndpoints = {
            "/api-docs",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs"
        };

        for (String endpoint : swaggerEndpoints) {
            logger.debug("Testing access to endpoint: {}", endpoint);
            MvcResult result = mockMvc.perform(get(endpoint))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
            
            int status = result.getResponse().getStatus();
            logger.debug("Response status for {}: {}", endpoint, status);
            assertTrue(status != 401 && status != 403, 
                String.format("Endpoint %s returned status %d", endpoint, status));
        }
    }

    @Test
    @DisplayName("Should allow access to actuator endpoints without authentication")
    void shouldAllowActuatorAccess() throws Exception {
        logger.debug("Testing access to actuator endpoints");
        
        MvcResult result = mockMvc.perform(get("/actuator/health"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        logger.debug("Actuator health check response content: {}", content);
        
        assertTrue(content.contains("\"status\""), "Response should contain health status");
        assertTrue(content.contains("\"components\""), "Response should contain health components");
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void shouldRequireAuthForProtectedEndpoints() throws Exception {
        logger.debug("Testing access to protected endpoints");
        
        String[] protectedEndpoints = {
            "/api/users",
            "/api/games",
            "/api/ratings"
        };

        for (String endpoint : protectedEndpoints) {
            logger.debug("Testing access to protected endpoint: {}", endpoint);
            mockMvc.perform(get(endpoint)
                    .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
        }
    }

    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowPublicEndpointsAccess() throws Exception {
        logger.debug("Testing access to public endpoints");
        
        String[] publicEndpoints = {
            "/api/users/register",
            "/api/users/verify",
            "/api/auth/login"
        };

        for (String endpoint : publicEndpoints) {
            logger.debug("Testing access to public endpoint: {}", endpoint);
            mockMvc.perform(post(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status != 401 && status != 403,
                        String.format("Public endpoint %s should not require auth, but got status %d", 
                            endpoint, status));
                });
        }
    }

    @Test
    @DisplayName("Should configure OAuth2 login endpoints correctly")
    void shouldConfigureOAuth2LoginEndpoints() throws Exception {
        logger.debug("Testing OAuth2 login endpoint configuration");
        
        String[] oauth2Endpoints = {
            "/login/oauth2/code/google",
            "/oauth2/authorization/google"
        };

        for (String endpoint : oauth2Endpoints) {
            logger.debug("Testing access to OAuth2 endpoint: {}", endpoint);
            MvcResult result = mockMvc.perform(get(endpoint))
                .andDo(MockMvcResultHandlers.print())
                .andReturn();
            
            int status = result.getResponse().getStatus();
            logger.debug("Response status for {}: {}", endpoint, status);
            assertTrue(status != 401 && status != 403, 
                String.format("OAuth2 endpoint %s should not require auth, but got status %d", 
                    endpoint, status));
        }
    }

    @Test
    void testOAuth2SuccessEndpoint_ShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/success"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testOAuth2FailureEndpoint_ShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/failure"))
                .andExpect(status().isOk());
    }
} 
