package com.gamerecs.back.service;

import com.gamerecs.back.util.BaseUnitTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailServiceTest extends BaseUnitTest {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceTest.class);

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_FROM_EMAIL = "noreply@test.com";
    private static final String TEST_BASE_URL = "http://localhost:4200";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_HTML_CONTENT = "<html>Test Email Content</html>";

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment for EmailService");
        
        // Set required properties using ReflectionTestUtils since they're normally injected via @Value
        ReflectionTestUtils.setField(emailService, "fromEmail", TEST_FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", TEST_BASE_URL);
    }

    @Test
    @DisplayName("Should send verification email successfully")
    void shouldSendVerificationEmail() throws MessagingException {
        logger.debug("Testing successful verification email sending");

        // Set up test-specific mock behavior
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        // Execute the method
        emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN);

        // Verify template engine was called with correct context
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());
        
        Context capturedContext = contextCaptor.getValue();
        assertEquals(TEST_USERNAME, capturedContext.getVariable("username"), 
                "Username should be set in template context");
        assertEquals(TEST_BASE_URL + "/auth/verify-email?token=" + TEST_TOKEN, 
                capturedContext.getVariable("verificationLink"), 
                "Verification link should be correctly constructed");

        // Verify email was sent with correct parameters
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should generate unique verification tokens")
    void shouldGenerateUniqueVerificationTokens() {
        logger.debug("Testing verification token generation");

        String token1 = emailService.generateVerificationToken();
        String token2 = emailService.generateVerificationToken();

        assertNotNull(token1, "Generated token should not be null");
        assertNotNull(token2, "Generated token should not be null");
        assertNotEquals(token1, token2, "Generated tokens should be unique");
        assertTrue(token1.matches("[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}"), 
                "Token should be a valid UUID");
    }

    @Test
    @DisplayName("Should handle email sending failure")
    void shouldHandleEmailSendingFailure() {
        logger.debug("Testing email sending failure handling");

        // Set up test-specific mock behavior
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Verify the exception is propagated
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN)
        );

        assertEquals("Failed to send email", exception.getMessage());
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle template processing failure")
    void shouldHandleTemplateProcessingFailure() {
        logger.debug("Testing template processing failure handling");

        // Set up test-specific mock behavior
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Verify the exception is propagated
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN)
        );

        assertEquals("Template processing failed", exception.getMessage());
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception for null email address")
    void shouldThrowExceptionForNullEmail() {
        logger.debug("Testing null email validation");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendVerificationEmail(null, TEST_USERNAME, TEST_TOKEN)
        );

        assertEquals("Email address cannot be null or empty", exception.getMessage());
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception for null username")
    void shouldThrowExceptionForNullUsername() {
        logger.debug("Testing null username validation");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendVerificationEmail(TEST_EMAIL, null, TEST_TOKEN)
        );

        assertEquals("Username cannot be null or empty", exception.getMessage());
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception for null token")
    void shouldThrowExceptionForNullToken() {
        logger.debug("Testing null token validation");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, null)
        );

        assertEquals("Verification token cannot be null or empty", exception.getMessage());
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception for malformed email address")
    void shouldThrowExceptionForMalformedEmail() {
        logger.debug("Testing malformed email validation");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendVerificationEmail("invalid-email", TEST_USERNAME, TEST_TOKEN)
        );

        assertEquals("Invalid email address format", exception.getMessage());
        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
} 
