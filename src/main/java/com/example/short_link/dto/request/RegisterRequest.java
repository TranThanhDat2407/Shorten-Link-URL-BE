package com.example.short_link.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}
