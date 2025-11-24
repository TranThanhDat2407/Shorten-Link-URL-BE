package com.example.short_link.service;

import com.example.short_link.dto.request.ChangePasswordRequest;
import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.UserSearchRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    User register(RegisterRequest request);

    void logout(String accessToken, String refreshToken, HttpServletResponse response);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    User findByEmail(String email);

    Page<User> searchUsers(UserSearchRequest request, Pageable pageable);

    @Transactional
    void changePassword(ChangePasswordRequest request);

    void generateAndSendOtp(String email);

    String verifyOtpAndGenerateResetToken(String email, String otp);

    void resetPasswordByToken(String email, String newPassword);
}
