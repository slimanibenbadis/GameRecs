package com.gamerecs.back.config;

import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
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
            mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized());
        }
    }
} 
