package com.example.short_link.controller;

import com.example.short_link.dto.request.*;
import com.example.short_link.dto.response.*;
import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.service.TokenService;
import com.example.short_link.service.UserService;
import com.example.short_link.util.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final RedisService redisService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        RegisterResponse response = RegisterResponse.builder()
                .message("Create user " + user.getEmail() + " successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        AuthResponse auth = userService.login(request, httpServletRequest);

        return ResponseEntity.ok(auth);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@RequestBody TokenRefreshRequest request) {
        String newAccessToken = tokenService.refreshAccessToken(
                request.getRefreshToken()
        );
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.builder()
                .accessToken(newAccessToken)
                .build();
        return ResponseEntity.ok(accessTokenResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response,
            @RequestBody(required = false) LogoutRequest request) {

        String accessToken = null;
        String refreshToken = null;

        // Extract access token
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7).trim();
        }

        // Extract refresh token
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            refreshToken = request.getRefreshToken().trim();
        }

        userService.logout(accessToken, refreshToken, response);

        return ResponseEntity.ok(LogoutResponse.builder()
                .success(true)
                .message("Đăng xuất thành công")
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/changePassword")
    public ResponseEntity<SimpleResponse> changePassword(
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(SimpleResponse.builder()
                .success(true)
                .message("Change Password Successfully")
                .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SimpleResponse> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        userService.generateAndSendOtp(request.getEmail());

        return ResponseEntity.ok(SimpleResponse.builder()
                .success(true)
                .message("Send OTP Successfully, Pls check your email!")
                .build()
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        String resetToken = userService.verifyOtpAndGenerateResetToken(
                request.getEmail(),
                request.getOtp()
        );

        return ResponseEntity.ok(
                VerifyOtpResponse.builder()
                        .resetToken(resetToken)
                        .expiresIn(900) // 15 phút
                        .message("Valid OTP, you can reset password in 15 mins")
                        .build()

        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SimpleResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = null;

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        String email = jwtService.validatePasswordResetToken(token); // sẽ throw nếu invalid

        userService.resetPasswordByToken(email, request.getNewPassword());

        return ResponseEntity.ok(SimpleResponse.builder()
                .success(true)
                .message("Reset Password Successfully! Pls login")
                .build()
        );
    }
}
