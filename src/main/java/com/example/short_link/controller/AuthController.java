package com.example.short_link.controller;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.TokenRefreshRequest;
import com.example.short_link.dto.response.AccessTokenResponse;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.dto.response.RegisterResponse;
import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.service.TokenService;
import com.example.short_link.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final TokenService tokenService;

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
            @RequestBody LoginRequest request){
        AuthResponse auth = userService.login(request);

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

    @PostMapping("/test")
    public ResponseEntity<?> test (
            @RequestBody RegisterRequest request){
        RegisterResponse response = RegisterResponse.builder()
                .message("Test successfully")
                .build();

        return ResponseEntity.ok(response);
    }

}
