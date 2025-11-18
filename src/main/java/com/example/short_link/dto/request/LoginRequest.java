package com.example.short_link.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
