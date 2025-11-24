package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private String resetToken;
    private long expiresIn;     // giây
    private String message;
}
