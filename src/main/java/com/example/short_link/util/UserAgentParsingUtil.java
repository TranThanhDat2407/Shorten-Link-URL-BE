package com.example.short_link.util;

import org.springframework.stereotype.Component;

@Component
public class UserAgentParsingUtil {
    public String getDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String lowerCaseAgent = userAgent.toLowerCase();

        // Kiểm tra Mobile/Tablet
        if (lowerCaseAgent.contains("mobile") || lowerCaseAgent.contains("android") || lowerCaseAgent.contains("ipad")) {
            return "Mobile/Tablet";
        }

        // Kiểm tra Robot/Bot
        if (lowerCaseAgent.contains("bot") || lowerCaseAgent.contains("spider") || lowerCaseAgent.contains("crawler")) {
            return "Bot/Crawler";
        }

        // Postman hoặc Desktop mặc định
        if (lowerCaseAgent.contains("postman")) return "Postman";

        return "Desktop";
    }

    /**
     * Phân tích chuỗi User-Agent để xác định trình duyệt.
     */
    public String getBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";

        if (userAgent.contains("Edg")) return "Edge";
        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";

        // Postman
        if (userAgent.contains("PostmanRuntime")) return "Postman";

        return "Other";
    }
}
