package com.example.short_link.dto.request;

import lombok.Data;

@Data
public class CreateShortCodeRequest {
    private String originalUrl;
}
