package com.example.short_link.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLinkRequest {
    @NotBlank
    private String originalUrl;
}
