package com.example.short_link.sercurity.filter;

import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.util.CookiesUtil;
import com.example.short_link.util.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtBlackListFilter extends OncePerRequestFilter {
    private final RedisService redisService;
    private final CookiesUtil cookiesUtil;
    private final JwtService jwtService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        log.info(">>> JwtAuthenticationFilter triggered for: {}", request.getRequestURI());

        String jwt = null;
        String cookieToken = cookiesUtil.getCookieValue(request, "access_token");
        if (StringUtils.hasText(cookieToken)) {
            jwt = cookieToken;
            log.info(">>> Token taken from HttpOnly cookie");
        }

        //  Nếu không có cookie → fallback lấy từ header (Mobile, Postman, API)
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                log.info(">>> Token taken from Authorization header");
            }
        }
        

        log.info(">>> Đã vào JwtBlackListFilter");

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        //  Extract jti
        String jti = jwtService.extractJti(jwt);
        //  Kiểm tra blacklist jti
        if (jti != null && redisService.isTokenBlacklisted(jti)) {
            return;
        }

        filterChain.doFilter(request, response);

    }
}
