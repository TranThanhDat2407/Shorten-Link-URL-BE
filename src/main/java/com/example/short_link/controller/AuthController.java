package com.example.short_link.controller;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.LogoutRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.TokenRefreshRequest;
import com.example.short_link.dto.response.AccessTokenResponse;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.dto.response.LogoutResponse;
import com.example.short_link.dto.response.RegisterResponse;
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
    public ResponseEntity<RegisterResponse> register (
            @Valid @RequestBody RegisterRequest request){
        User user = userService.register(request);
        RegisterResponse response = RegisterResponse.builder()
                .message("Create user " + user.getEmail() + " successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login (
            @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest){
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

}
