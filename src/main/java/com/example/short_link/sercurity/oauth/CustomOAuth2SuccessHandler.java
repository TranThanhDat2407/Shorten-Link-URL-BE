package com.example.short_link.sercurity.oauth;

import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import com.example.short_link.enums.Role;
import com.example.short_link.repository.UserRepository;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.sercurity.user.CustomUserDetails;
import com.example.short_link.sercurity.user.CustomUserDetailsService;
import com.example.short_link.service.TokenService;
import com.example.short_link.util.CookiesUtil;
import com.example.short_link.util.UserAgentParsingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserAgentParsingUtil userAgentUtil;
    private final CookiesUtil cookiesUtil;

    @Value("${spring.application.frontend-domain}")
    private String frontEndDomain;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OidcUser oAuth2User = (OidcUser) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 1. Tìm user trong DB
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            //fullname = subtring trước @
                            .fullName(name)
                            .role(Role.USER)
                            .provider(AuthProvider.GOOGLE)
                            .pictureUrl(picture)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        //TẠO ACCESS TOKEN
        String accessToken = jwtService.generateAccessToken(userDetails);

        // 6. Tạo refresh token mới
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        Instant expiresAt = jwtService.extractExpiration(refreshToken);

        String userAgentHeader = request.getHeader("User-Agent");
        String deviceName = userAgentUtil.getDevice(userAgentHeader)
                + " • " + userAgentUtil.getBrowser(userAgentHeader);
        String ipAddress = request.getRemoteAddr();


        //giới hạn token mỗi device
        tokenService.limitTokensPerDevice(user, deviceName, 2);

        // Lưu Refresh Token
        tokenService.saveUserToken(user, refreshToken, expiresAt, deviceName, ipAddress);

        cookiesUtil.setCookie(response, "access_token",
                accessToken, 15 * 60);   // 15 phút
        cookiesUtil.setCookie(response, "refresh_token",
                refreshToken, 7 * 24 * 60 * 60); // 7 ngày

        AuthResponse authResponse = AuthResponse.builder()
                .fullName(user.getFullName())
                .role(user.getRole().toString())
                .pictureUrl(user.getPictureUrl())
                .build();

        // Redirect về frontend + mang theo data
        String redirectUrl = frontEndDomain + "/google-success?data=" +
                URLEncoder.encode(
                        new ObjectMapper().writeValueAsString(authResponse)
                        , StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
