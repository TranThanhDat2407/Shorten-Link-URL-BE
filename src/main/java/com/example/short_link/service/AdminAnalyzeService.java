package com.example.short_link.service;

import com.example.short_link.dto.response.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public interface AdminAnalyzeService {
    DashboardResponse getDashboard();

    List<DailyClickResponse> getLast7DaysClicks();

    List<DailyLinksResponse> getLast7DaysLinks();

    List<TopLinkResponse> getTopLinks(Integer limit, LocalDate from, LocalDate to);

    LinkAnalyticsResponse getLinkAnalytics(String shortCode, LocalDate from, LocalDate to);


}
