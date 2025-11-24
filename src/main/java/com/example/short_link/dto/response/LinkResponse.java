package com.example.short_link.dto.response;

import com.example.short_link.entity.Link;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@Builder
public class LinkResponse {
    private Long id;
    private String originalUrl;
    private String shortCode;
    private String qrCodeUrl;
    private Boolean isQrGenerated;
    private Long clickCount;
    private Instant expiredAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String userEmail;

    public static LinkResponse fromEntity(Link link){
        return LinkResponse.builder()
                .id(link.getId())
                .originalUrl(link.getOriginalUrl())
                .shortCode(link.getShortCode())
                .qrCodeUrl(link.getQrCodeUrl())
                .isQrGenerated(link.isQrGenerated())
                .clickCount(link.getClickCount() == null ? 0 : link.getClickCount())
                .expiredAt(link.getExpiredAt() == null ?
                        Instant.now().plus(10, ChronoUnit.DAYS) : link.getExpiredAt())
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .userEmail(link.getUser() == null ? " " : link.getUser().getEmail())
                .build();
    }
}
