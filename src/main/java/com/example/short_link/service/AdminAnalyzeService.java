package com.example.short_link.service;

import com.example.short_link.dto.response.DailyClickResponse;
import com.example.short_link.dto.response.DashboardResponse;
import com.example.short_link.dto.response.LinkAnalyticsResponse;
import com.example.short_link.dto.response.TopLinkResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface AdminAnalyzeService {
    DashboardResponse getDashboard();

    List<DailyClickResponse> getLast7DaysClicks();

    List<TopLinkResponse> getTopLinks(Integer limit, LocalDate from, LocalDate to);

    LinkAnalyticsResponse getLinkAnalytics(String shortCode, LocalDate from, LocalDate to);


}
