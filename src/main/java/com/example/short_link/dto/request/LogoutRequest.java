package com.example.short_link.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
