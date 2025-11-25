package com.example.short_link.controller;

import com.example.short_link.dto.response.DailyClickResponse;
import com.example.short_link.dto.response.DashboardResponse;
import com.example.short_link.dto.response.LinkAnalyticsResponse;
import com.example.short_link.dto.response.TopLinkResponse;
import com.example.short_link.service.AdminAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("${api.prefix}/admin/analytics")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyzeController {
    private final AdminAnalyzeService analyzeService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return analyzeService.getDashboard();
    }

    @GetMapping("/chart/7days")
    public List<DailyClickResponse> chart7Days() {
        return analyzeService.getLast7DaysClicks();
    }

    @GetMapping("/top-links")
    public List<TopLinkResponse> topLinks(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return analyzeService.getTopLinks(limit, from, to);
    }

    @GetMapping("/link/{shortCode}")
    public LinkAnalyticsResponse linkDetail(
            @PathVariable String shortCode,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return analyzeService.getLinkAnalytics(shortCode, from, to);
    }
}
