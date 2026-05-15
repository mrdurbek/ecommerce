package com.ecommerce.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Please verify your email address";
        String htmlContent = buildVerificationEmailHtml(firstName, verificationUrl);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendPasswordChangedNotification(String toEmail, String firstName) {
        String subject = "Your password has been changed";
        String htmlContent = buildPasswordChangedEmailHtml(firstName);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(String firstName, String verificationUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2>Hello, %s!</h2>
                    <p>Thank you for registering. Please verify your email address by clicking the button below:</p>
                    <a href="%s" style="background-color: #4CAF50; color: white; padding: 14px 25px;
                        text-decoration: none; border-radius: 4px; display: inline-block;">
                        Verify Email
                    </a>
                    <p>This link will expire in 24 hours.</p>
                    <p>If you didn't create this account, please ignore this email.</p>
                </body>
                </html>
                """.formatted(firstName, verificationUrl);
    }

    private String buildPasswordChangedEmailHtml(String firstName) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2>Hello, %s!</h2>
                    <p>Your password has been successfully changed.</p>
                    <p>If you didn't make this change, please contact our support team immediately.</p>
                </body>
                </html>
                """.formatted(firstName);
    }
}