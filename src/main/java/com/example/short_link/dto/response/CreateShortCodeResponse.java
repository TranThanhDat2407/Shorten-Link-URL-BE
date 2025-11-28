package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateShortCodeResponse {
    private String shortUrl;
    private String code;
    private String originalUrl;
    private String qrCodeUrl;
}
