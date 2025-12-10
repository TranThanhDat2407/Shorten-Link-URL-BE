package com.example.short_link.util;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
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
//    private final JavaMailSender mailSender;
//
//    @Value("${spring.mail.username}")
//    private String fromEmail;
//
//    public void sendOtpEmail(String to, String otp, Instant expiresAt) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//            helper.setTo(to);
//            helper.setSubject("Your ShortLink Verification OTP");
//            helper.setFrom(fromEmail);
//
//            String htmlBody = """
//                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
//                        <h2 style="color: #2E8B57;">ShortLink Account Verification</h2>
//                        <p>Your One-Time Password (OTP) is:</p>
//                        <h1 style="font-size: 32px; letter-spacing: 8px; color: #2E8B57;">%s</h1>
//                        <p>This OTP will expire at <strong>%s</strong>.</p>
//                        <p>If you did not request this code, please ignore this email.</p>
//                        <hr>
//                        <small>ShortLink Team &copy; 2025</small>
//                    </div>
//                    """.formatted(
//                    otp,
//                    DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
//                            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
//                            .format(expiresAt)
//            );
//
//            helper.setText(htmlBody, true);
//            mailSender.send(message);
//
//            log.info("OTP email sent successfully to {}", to);
//        } catch (Exception e) {
//            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
//        }
//    }

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.sender-email}")
    private String senderEmail;

    public void sendOtpEmail(String to, String otp, Instant expiresAt) {

        MailjetClient client = new MailjetClient(apiKey, secretKey);

        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #2E8B57;">ShortLink Account Verification</h2>
                    <p>Your One-Time Password (OTP) is:</p>
                    <h1 style="font-size: 32px; letter-spacing: 8px; color: #2E8B57;">%s</h1>
                    <p>This OTP will expire at <strong>%s</strong>.</p>
                    <p>If you did not request this code, please ignore this email.</p>
                    <hr>
                    <small>ShortLink Team © 2025</small>
                </div>
                """.formatted(
                otp,
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
                        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(expiresAt)
        );

        try {
            JSONObject message = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", senderEmail)
                            .put("Name", "ShortLink"))
                    .put(Emailv31.Message.TO, new JSONArray()
                            .put(new JSONObject()
                                    .put("Email", to)))
                    .put(Emailv31.Message.SUBJECT, "Your ShortLink OTP")
                    .put(Emailv31.Message.HTMLPART, htmlBody);

            client.post(new com.mailjet.client.MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message)));

            log.info("OTP email sent successfully to {}", to);

        } catch (MailjetException e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
        }
    }
}

