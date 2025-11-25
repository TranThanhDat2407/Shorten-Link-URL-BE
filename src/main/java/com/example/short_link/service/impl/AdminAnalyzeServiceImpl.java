package com.example.short_link.service.impl;

import com.example.short_link.dto.response.DailyClickResponse;
import com.example.short_link.dto.response.DashboardResponse;
import com.example.short_link.dto.response.LinkAnalyticsResponse;
import com.example.short_link.dto.response.TopLinkResponse;
import com.example.short_link.entity.Link;
import com.example.short_link.repository.LinkClickLogRepository;
import com.example.short_link.repository.LinkRepository;
import com.example.short_link.service.AdminAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyzeServiceImpl implements AdminAnalyzeService {
    private final LinkRepository linkRepository;
    private final LinkClickLogRepository logRepository;

    //timezone vietnam ho chi minh
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    //hàm lấy đầu ngày
    private Instant startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay(ZONE).toInstant() : null;
    }

    //hàm lấy cuối ngày
    private Instant endOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59, 999999999)
                .atZone(ZONE).toInstant() : null;
    }

    @Override
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(ZONE);
        Instant start = today.atStartOfDay(ZONE).toInstant();
        Instant end = today.atTime(23, 59, 59, 999999999)
                .atZone(ZONE).toInstant();

        return DashboardResponse.builder()
                .totalLinks(linkRepository.count())
                .totalClicksAllTime(linkRepository.sumAllClickCounts() !=
                        null ? linkRepository.sumAllClickCounts() : 0L)
                .todayClicks(logRepository.countTodayClicks())
                .todayNewLinks(linkRepository.countByCreatedAtBetween(start, end))
                .build();
    }

    @Override
    public List<DailyClickResponse> getLast7DaysClicks() {
        LocalDate end = LocalDate.now(ZONE);
        LocalDate start = end.minusDays(6);

        return logRepository.countClicksByDateRaw(startOfDay(start), endOfDay(end))
                .stream()
                .map(row -> DailyClickResponse.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .clicks((Long) row[1])
                        .build())
                .toList();
    }

    @Override
    public List<TopLinkResponse> getTopLinks(Integer limit, LocalDate from, LocalDate to) {
        limit = (limit == null || limit <= 0) ? 10 : limit;

        List<Object[]> raw;
        if (from != null || to != null) {
            Instant start = from != null ? from.atStartOfDay(ZONE).toInstant() : Instant.EPOCH;
            Instant end = to != null ? to.atTime(23, 59, 59, 999999999).atZone(ZONE).toInstant() : Instant.now();
            raw = logRepository.findTopLinksWithRange(start, end);
        } else {
            raw = logRepository.findTopLinksAllTime();
        }

        return raw.stream()
                .limit(limit)
                .map(row -> TopLinkResponse.builder()
                        .shortCode((String) row[0])
                        .originalUrl((String) row[1])
                        .clicks((Long) row[2])
                        .build())
                .toList();
    }

    @Override
    public LinkAnalyticsResponse getLinkAnalytics(String shortCode, LocalDate from, LocalDate to) {
        Link link = linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        Instant start = from != null ? from.atStartOfDay(ZONE).toInstant() : null;
        Instant end = to != null ? to.atTime(23, 59, 59, 999999999)
                .atZone(ZONE).toInstant() : null;

        boolean hasRange = start != null || end != null;

        Long totalClicks = hasRange
                ? logRepository.countByLinkAndRange(link, start, end)
                : logRepository.countByLinkAllTime(link);

        Long uniqueVisitors = hasRange
                ? logRepository.countDistinctIpWithRange(link, start, end)
                : logRepository.countDistinctIpAllTime(link);

        List<Object[]> dailyRaw = hasRange
                ? logRepository.countDailyByLinkWithRange(link, start, end)
                : logRepository.countDailyByLinkAllTime(link);

        List<DailyClickResponse> dailyClicks = dailyRaw.stream()
                .map(row -> DailyClickResponse.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .clicks((Long) row[1])
                        .build())
                .toList();

        return LinkAnalyticsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(link.getOriginalUrl())
                .totalClicks(totalClicks != null ? totalClicks : 0L)
                .uniqueVisitors(uniqueVisitors != null ? uniqueVisitors : 0L)
                .dailyClicks(dailyClicks)
                .build();
    }
}
