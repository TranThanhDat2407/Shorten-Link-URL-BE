package com.example.short_link.controller;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import com.example.short_link.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<?> register (
            @RequestBody RegisterRequest request){
        User user = userService.register(request);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login (
            @RequestBody LoginRequest request){
        AuthResponse auth = userService.login(request);

        return ResponseEntity.ok(auth);
    }
}
