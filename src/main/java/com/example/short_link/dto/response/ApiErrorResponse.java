package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApiErrorResponse {
    private int status;
    private String message;
    private String errorCode;
    private Instant timestamp;
}
