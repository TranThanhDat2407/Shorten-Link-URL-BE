package com.example.short_link.dto.request;

import lombok.Data;

import java.time.Instant;

@Data
public class LinkSearchRequest {
    private String userId;           // null = guest, có giá trị = user cụ thể, không truyền = tất cả
    private String shortCode;
    private String originalUrl;
    private Instant createdFrom;
    private Instant createdTo;

    public Long getUserIdAsLong() {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        String trimmed = userId.trim();
        if ("null".equalsIgnoreCase(trimmed)) {
            return null;                    // userId=null → lấy link guest
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return -1L;                     // giá trị không hợp lệ → bỏ filter user
        }
    }
}
