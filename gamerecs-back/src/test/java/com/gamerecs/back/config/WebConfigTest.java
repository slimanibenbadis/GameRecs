package com.gamerecs.back.config;

import com.gamerecs.back.util.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebConfigTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(WebConfigTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Test
    @DisplayName("Should configure CORS with correct allowed origins")
    void shouldConfigureCorsWithAllowedOrigins() throws Exception {
        logger.debug("Testing CORS configuration with allowed origins: {}", allowedOrigins);

        mockMvc.perform(options("/api/test")
                .header("Origin", allowedOrigins)
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigins))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Should allow all configured HTTP methods")
    void shouldAllowConfiguredHttpMethods() throws Exception {
        logger.debug("Testing CORS configuration for allowed HTTP methods");

        String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};
        for (String method : allowedMethods) {
            logger.debug("Testing CORS for HTTP method: {}", method);
            mockMvc.perform(options("/api/test")
                    .header("Origin", allowedOrigins)
                    .header("Access-Control-Request-Method", method))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Methods"));
        }
    }

    @Test
    @DisplayName("Should allow all headers")
    void shouldAllowAllHeaders() throws Exception {
        logger.debug("Testing CORS configuration for allowed headers");

        String[] testHeaders = {"Content-Type", "Authorization", "X-Requested-With"};
        for (String header : testHeaders) {
            logger.debug("Testing CORS for header: {}", header);
            mockMvc.perform(options("/api/test")
                    .header("Origin", allowedOrigins)
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", header))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Headers"));
        }
    }

    @Test
    @DisplayName("Should set correct max age")
    void shouldSetCorrectMaxAge() throws Exception {
        logger.debug("Testing CORS configuration for max age");

        mockMvc.perform(options("/api/test")
                .header("Origin", allowedOrigins)
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    @DisplayName("Should reject non-allowed origins")
    void shouldRejectNonAllowedOrigins() throws Exception {
        logger.debug("Testing CORS configuration rejection of non-allowed origins");

        String nonAllowedOrigin = "http://malicious-site.com";
        logger.debug("Testing with non-allowed origin: {}", nonAllowedOrigin);

        mockMvc.perform(options("/api/test")
                .header("Origin", nonAllowedOrigin)
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
} 
