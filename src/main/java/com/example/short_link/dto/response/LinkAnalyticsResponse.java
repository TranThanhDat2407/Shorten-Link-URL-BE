package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LinkAnalyticsResponse {
    String shortCode;
    String originalUrl;
    Long totalClicks;
    Long uniqueVisitors;
    List<DailyClickResponse> dailyClicks;
}
