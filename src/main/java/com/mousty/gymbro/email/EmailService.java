package com.mousty.gymbro.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to GymBro!");
            message.setText("Hello " + name + ", welcome to GymBro!");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage(), e);
        }
    }

    public void sendResetPasswordEmail(String toEmail, String name,String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset password");
            message.setText("Hi %s !\n this is your temporary password: %s \n reset it once u logged in"
                    .formatted(name,password));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email: " + e.getMessage(), e);
        }
    }

    public void sendResetOtpEmail(String toEmail, String name,String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset OTP");
            message.setText("Hi %s ! \n this is your temporary OTP: %s.\n use this OTP to reset your password."
                    .formatted(name,otp));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email: " + e.getMessage(), e);
        }
    }

    public void sendOtpEmail(String toEmail, String name,String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Account Verification OTP");
            message.setText("Hi %s ! \n this is your temporary OTP: %s.\n use this OTP to verify your account."
                    .formatted(name,otp));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send account verification email: " + e.getMessage(), e);
        }
    }


}
