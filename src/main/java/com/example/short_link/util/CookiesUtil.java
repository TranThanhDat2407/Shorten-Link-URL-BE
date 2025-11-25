package com.example.short_link.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookiesUtil {

    public void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);                    // Quan trọng: chống JS đọc
        cookie.setSecure(false);                      // Chỉ gửi qua HTTPS (dev thì false tạm)
        cookie.setPath("/");                         // Toàn domain
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public void revokeCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);                 // Quan trọng: maxAge = 0 → xóa ngay
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public void revokeCookies(HttpServletResponse response, String... cookieNames) {
        for (String name : cookieNames) {
            revokeCookie(response, name);
        }
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
