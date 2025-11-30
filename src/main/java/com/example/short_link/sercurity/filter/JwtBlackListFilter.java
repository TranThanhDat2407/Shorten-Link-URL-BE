package com.example.short_link.sercurity.filter;

import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.util.CookiesUtil;
import com.example.short_link.util.RedisService;
import io.jsonwebtoken.ExpiredJwtException;
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

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            //  Extract jti (Nơi exception được ném ra)
            String jti = jwtService.extractJti(jwt);

            //  Kiểm tra blacklist jti
            if (jti != null && redisService.isTokenBlacklisted(jti)) {
                log.warn(">>> Blocked request due to blacklisted JTI: {}", jti);
                // Nếu bị blacklist, bạn phải CHẶN request ở đây
                return; // Dừng chuỗi filter
            }

        } catch (ExpiredJwtException ex) {
            // Token hết hạn. Cho phép request đi tiếp đến JwtAuthenticationFilter
            // để JwtAuthenticationFilter xử lý logic trả về 401.
            log.debug(">>> Token expired detected in Blacklist Filter. Proceeding to Auth Filter.");

        } catch (Exception ex) {
            // Bắt lỗi SignatureException hoặc lỗi parsing khác
            log.warn(">>> Invalid token (Signature/Parsing Error) in Blacklist Filter: {}", ex.getMessage());
            // Cho phép request đi tiếp để JwtAuthenticationFilter xử lý (nếu nó có thể)
            // hoặc để filter tiếp theo (JwtAuthenticationFilter) thất bại và không đặt Context.
        }

        // Nếu không bị chặn (blacklist) và không có exception thoát ra:
        filterChain.doFilter(request, response);
    }
}
