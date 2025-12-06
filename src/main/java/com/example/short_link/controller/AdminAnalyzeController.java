package com.example.short_link.controller;

import com.example.short_link.dto.response.*;
import com.example.short_link.service.AdminAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(analyzeService.getDashboard());
    }

    @GetMapping("/chart/7days")
    public ResponseEntity<List<DailyClickResponse>> chart7Days() {
        return ResponseEntity.ok(analyzeService.getLast7DaysClicks());
    }

    @GetMapping("/chart/7days/link-created")
    public ResponseEntity<List<DailyLinksResponse>> chart7DaysLinkCreated() {
        return ResponseEntity.ok(analyzeService.getLast7DaysLinks());
    }

    @GetMapping("/top-links")
    public ResponseEntity<List<TopLinkResponse>> topLinks(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(analyzeService.getTopLinks(limit, from, to));
    }

    @GetMapping("/link/{shortCode}")
    public ResponseEntity<LinkAnalyticsResponse> linkDetail(
            @PathVariable String shortCode,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(analyzeService.getLinkAnalytics(shortCode, from, to));
    }
}
