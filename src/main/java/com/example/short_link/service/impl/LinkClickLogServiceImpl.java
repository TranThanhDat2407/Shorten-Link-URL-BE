package com.example.short_link.service.impl;

import com.example.short_link.entity.Link;
import com.example.short_link.entity.LinkClickLog;
import com.example.short_link.repository.LinkClickLogRepository;
import com.example.short_link.service.LinkClickLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LinkClickLogServiceImpl implements LinkClickLogService {
    private final LinkClickLogRepository linkClickLogRepository;

    @Override
    public void logClickDetails(Link link, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();


        String device = getDeviceFromUserAgent(userAgent);
        String browser = getBrowserFromUserAgent(userAgent);
        String country = getCountryFromIp(ipAddress);
        String city = getCityFromIp(ipAddress);

        LinkClickLog log =  LinkClickLog.builder()
                .clicked_at(Instant.now())
                .ip(ipAddress)
                .country(country)
                .city(city)
                .browser(browser)
                .device(device)
                .link(link)
                .build();

        linkClickLogRepository.save(log);
    }


    private String getDeviceFromUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.toLowerCase().contains("mobile")) return "Mobile";
        return "Desktop";
    }

    private String getBrowserFromUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) return "Chrome";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
        return "Other";
    }

    private String getCountryFromIp(String ipAddress) {

        return "Vietnam";
    }

    private String getCityFromIp(String ipAddress) {

        return "HoChiMinh";
    }
}
