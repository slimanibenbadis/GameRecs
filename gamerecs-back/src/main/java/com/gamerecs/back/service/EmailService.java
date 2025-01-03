package com.gamerecs.back.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

/**
 * Service for handling email-related operations.
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.properties.mail.smtp.from:noreply@localhost}")
    private String fromEmail;
    
    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;
    
    @Autowired
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Sends a verification email to a newly registered user.
     *
     * @param to the recipient's email address
     * @param username the recipient's username
     * @param token the verification token
     * @throws MessagingException if there is an error sending the email
     */
    public void sendVerificationEmail(String to, String username, String token) throws MessagingException {
        logger.debug("Preparing verification email for user: {}", username);
        logger.debug("Using sender email address: {}", fromEmail);
        
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("verificationLink", verificationBaseUrl + "/verify?token=" + token);
        
        String htmlContent = templateEngine.process("verification-email", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Verify Your GameRecs Account");
        helper.setText(htmlContent, true);
        
        logger.debug("Sending verification email to: {}", to);
        mailSender.send(message);
        logger.info("Verification email sent successfully to: {}", to);
    }
    
    /**
     * Generates a random verification token.
     *
     * @return a random UUID string
     */
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
} 
