package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopLinkResponse {
    String shortCode;
    String originalUrl;
    Long clicks;
}
