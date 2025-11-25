package com.example.short_link.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {
    Long totalLinks;
    Long totalClicksAllTime;
    Long todayClicks;
    Long todayNewLinks;
}
