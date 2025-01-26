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
    void shouldHandleEmailSendingFailure() throws MessagingException {
        logger.debug("Testing email sending failure handling");

        // Set up test-specific mock behavior
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);
        
        // Simulate a MessagingException during email sending
        MessagingException testException = new MessagingException("Test email sending failure");
        doAnswer(invocation -> {
            throw new RuntimeException("Failed to send email", testException);
        }).when(mailSender).send(any(MimeMessage.class));

        // Execute and verify
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN),
                "Should throw RuntimeException when email sending fails");

        assertEquals("Failed to send email", thrown.getMessage(),
                "Exception message should match expected");
        assertSame(testException, thrown.getCause(),
                "Original MessagingException should be the cause");

        // Verify logging behavior
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("verification-email"), any(Context.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle specific MessagingException scenarios")
    void shouldHandleSpecificMessagingExceptionScenarios() throws MessagingException {
        logger.debug("Testing specific MessagingException scenarios");

        // Set up test-specific mock behavior
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        // Test cases for different MessagingException scenarios
        String[] errorMessages = {
            "Invalid email address",
            "SMTP server connection failed",
            "Authentication failed",
            "Message too large"
        };

        for (String errorMessage : errorMessages) {
            // Simulate specific MessagingException
            MessagingException specificException = new MessagingException(errorMessage);
            doAnswer(invocation -> {
                throw new RuntimeException("Failed to send email", specificException);
            }).when(mailSender).send(any(MimeMessage.class));

            // Execute and verify each scenario
            RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                    emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN),
                    "Should throw RuntimeException for: " + errorMessage);

            assertEquals("Failed to send email", thrown.getMessage(),
                    "Exception message should match expected for: " + errorMessage);
            assertSame(specificException, thrown.getCause(),
                    "Original MessagingException should be the cause for: " + errorMessage);
        }

        // Verify the number of attempts
        verify(mailSender, times(errorMessages.length)).send(any(MimeMessage.class));
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

    @Test
    @DisplayName("Should handle long email addresses")
    void shouldHandleLongEmailAddresses() throws MessagingException {
        logger.debug("Testing email sending with long email address");

        String longLocalPart = "a".repeat(64); // Max local part length
        String longDomain = "b".repeat(63); // Max domain label length
        String longEmail = longLocalPart + "@" + longDomain + ".com";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        emailService.sendVerificationEmail(longEmail, TEST_USERNAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle email addresses with special characters")
    void shouldHandleEmailWithSpecialCharacters() throws MessagingException {
        logger.debug("Testing email sending with special characters");

        String emailWithSpecialChars = "user.name+label@example.com";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        emailService.sendVerificationEmail(emailWithSpecialChars, TEST_USERNAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should verify email content structure")
    void shouldVerifyEmailContentStructure() throws MessagingException {
        logger.debug("Testing email content structure");

        // Create mock for MimeMessage with proper return values
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        // Mock the getFrom, getAllRecipients, and getSubject methods
        when(mockMessage.getFrom()).thenReturn(new jakarta.mail.internet.InternetAddress[]{
            new jakarta.mail.internet.InternetAddress(TEST_FROM_EMAIL)
        });
        when(mockMessage.getAllRecipients()).thenReturn(new jakarta.mail.internet.InternetAddress[]{
            new jakarta.mail.internet.InternetAddress(TEST_EMAIL)
        });
        when(mockMessage.getSubject()).thenReturn("Verify Your Gamer-Reco Account");

        emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN);

        // Verify the email was sent
        verify(mailSender).send(mockMessage);

        // Verify email headers
        assertEquals(TEST_FROM_EMAIL, mockMessage.getFrom()[0].toString(), 
                "From address should match");
        assertEquals(TEST_EMAIL, mockMessage.getAllRecipients()[0].toString(), 
                "To address should match");
        assertTrue(mockMessage.getSubject().contains("Verify"), 
                "Subject should contain verification keyword");
    }

    @Test
    @DisplayName("Should verify template context variables")
    void shouldVerifyTemplateContextVariables() throws MessagingException {
        logger.debug("Testing template context variables");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        emailService.sendVerificationEmail(TEST_EMAIL, TEST_USERNAME, TEST_TOKEN);

        verify(templateEngine).process(eq("verification-email"), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();

        // Verify all required template variables
        assertNotNull(capturedContext.getVariable("username"), 
                "Username should be present in template context");
        assertNotNull(capturedContext.getVariable("verificationLink"), 
                "Verification link should be present in template context");
        
        String verificationLink = (String) capturedContext.getVariable("verificationLink");
        assertTrue(verificationLink.contains(TEST_TOKEN), 
                "Verification link should contain the token");
        assertTrue(verificationLink.startsWith(TEST_BASE_URL), 
                "Verification link should start with base URL");
    }

    @Test
    @DisplayName("Should handle international email addresses")
    void shouldHandleInternationalEmailAddresses() throws MessagingException {
        logger.debug("Testing email sending with international characters");

        String internationalEmail = "user@例子.com";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("verification-email"), any(Context.class)))
                .thenReturn(TEST_HTML_CONTENT);

        emailService.sendVerificationEmail(internationalEmail, TEST_USERNAME, TEST_TOKEN);

        verify(mailSender).send(mimeMessage);
    }
} 
