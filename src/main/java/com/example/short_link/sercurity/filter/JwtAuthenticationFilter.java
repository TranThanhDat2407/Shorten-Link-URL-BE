package com.example.short_link.sercurity.filter;

import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.util.CookiesUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CookiesUtil cookiesUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.info(">>> JwtAuthenticationFilter triggered for: {}", request.getRequestURI());

        String jwt = null;
        String username = null;

        // === 1. Ưu tiên lấy token từ HttpOnly Cookie (WEB - AN TOÀN NHẤT) ===
        String cookieToken = cookiesUtil.getCookieValue(request, "access_token");
        if (StringUtils.hasText(cookieToken)) {
            jwt = cookieToken;
            log.info(">>> Token taken from HttpOnly cookie");
        }

        // === 2. Nếu không có cookie → fallback lấy từ header (Mobile, Postman, API) ===
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                log.info(">>> Token taken from Authorization header");
            }
        }

        // === 3. Không có token → bỏ qua (public route) ===
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtService.extractEmail(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.info(">>> Authenticated user: {} | Roles: {}", username, userDetails.getAuthorities());
                } else {
                    // Token invalid → xóa cookie nếu là web
                    if (cookieToken != null) {
                        cookiesUtil.revokeCookie(response, "access_token");
                    }
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            // Access token hết hạn → để interceptor xử lý refresh
            log.debug(">>> Access token expired for user: {}", username);
            // Không trả 401 ngay → để Angular tự gọi /refresh
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.warn(">>> Invalid token: {}", ex.getMessage());
            if (cookieToken != null) {
                cookiesUtil.revokeCookie(response, "access_token");
            }
        }
    }
}
