package com.example.short_link.sercurity.filter;

import com.example.short_link.util.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtBlackListFilter extends OncePerRequestFilter {
    private final RedisService redisService;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        log.info(">>> Đã vào JwtBlackListFilter");
        String jwt = authHeader.substring(7);

        if (jwt != null && redisService.isAccessTokenBlacklisted(jwt)) {
            log.warn("Token bị blacklist: {}", jwt.substring(0, 30) + "...");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"TOKEN_REVOKED\",\"message\"" +
                    ":\"TOKEN REVOKED PLS LOGIN.\"}");
            return;
        }

        filterChain.doFilter(request, response);

    }
}
