package com.example.short_link.controller;

import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/user")
@RequiredArgsConstructor
public class UserController {

    @PostMapping("/test")
    public ResponseEntity<?> test (
            @RequestBody RegisterRequest request){
        RegisterResponse response = RegisterResponse.builder()
                .message("Test successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
