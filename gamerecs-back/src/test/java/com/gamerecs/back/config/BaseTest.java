package com.gamerecs.back.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base abstract class for all tests providing common configurations
 */
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseTest {
    // Common test utilities and helper methods can be added here
} 