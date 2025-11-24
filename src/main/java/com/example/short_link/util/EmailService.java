package com.example.short_link.util;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp, Instant expiresAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Mã OTP xác thực ShortLink");
            helper.setFrom(fromEmail);

            String htmlBody = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                        <h2 style="color: #2E8B57;">Xác minh tài khoản ShortLink</h2>
                        <p>Mã OTP của bạn là:</p>
                        <h1 style="font-size: 32px; letter-spacing: 8px; color: #2E8B57;">%s</h1>
                        <p>Mã này sẽ hết hạn vào <strong>%s</strong></p>
                        <p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email.</p>
                        <hr>
                        <small>ShortLink Team &copy; 2025</small>
                    </div>
                    """.formatted(otp, DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .format(expiresAt));

            helper.setText(htmlBody, true);
            mailSender.send(message);

            log.info("OTP email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            // Không throw → tránh làm crash request
        }
    }

}

