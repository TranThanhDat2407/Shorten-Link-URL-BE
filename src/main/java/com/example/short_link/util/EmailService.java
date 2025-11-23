package com.example.short_link.util;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            // 1. Tạo MimeMessage
            MimeMessage message = mailSender.createMimeMessage();

            // 2. Sử dụng MimeMessageHelper để dễ dàng thiết lập các thuộc tính
            // Tham số thứ hai (true) chỉ định rằng đây là email có nội dung multipart/HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Mã OTP Đặt lại Mật khẩu của Bạn");

            // --- Nội dung HTML để làm nổi bật OTP ---
            // Sử dụng <b> hoặc <strong> để in đậm, hoặc thêm style cho màu sắc/kích cỡ.
            // Dùng <br> cho ngắt dòng.

            String htmlBody = String.format(
                    "<html><body>" +
                            "<h4>Chào bạn,</h4>" +
                            "<p>Đây là mã OTP để đặt lại mật khẩu cho tài khoản của bạn:</p>" +
                            "<p>Mã OTP: <strong><span style='color: #d9534f; font-size: 18px; border: 1px solid #ccc; padding: 5px; border-radius: 4px;'>%s</span></strong></p>" +
                            "<p>Mã này sẽ hết hạn trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>" +
                            "<p>Trân trọng,<br>" +
                            "Đội ngũ Hỗ trợ</p>" +
                            "</body></html>",
                    otp
            );

            // 3. Thiết lập nội dung là HTML
            helper.setText(htmlBody, true); // Tham số 'true' thứ hai cho biết nội dung là HTML

            mailSender.send(message);
            System.out.println("Email OTP đã được gửi thành công đến: " + toEmail);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email OTP đến " + toEmail + ": " + e.getMessage());
            // Tùy chọn: throw new RuntimeException("Gửi email thất bại", e);
        }
    }

}

