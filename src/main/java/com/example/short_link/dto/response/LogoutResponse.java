package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LogoutResponse {
    private boolean success;
    private String message;
    private Instant timestamp;
}
