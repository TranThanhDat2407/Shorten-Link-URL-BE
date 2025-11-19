package com.example.short_link.sercurity.oauth;

import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import com.example.short_link.enums.Role;
import com.example.short_link.repository.UserRepository;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.sercurity.user.CustomUserDetails;
import com.example.short_link.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenService tokenService;

    @Value("${spring.application.frontend-domain}")
    private String frontendDomain;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // 1. Tìm user trong DB
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            //fullname = subtring trước @
                            .fullName(name)
                            .role(Role.USER)
                            .provider(AuthProvider.GOOGLE)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. Generate token
        String accessToken = jwtService.generateAccessToken(
                new CustomUserDetails(user)
        );

        String refreshToken = jwtService.generateRefreshToken(
                new CustomUserDetails(user)
        );

        // Lưu Refresh Token
        tokenService.saveUserToken(user, refreshToken, jwtService.getRefreshTokenExpiryDate());

        long refreshTokenValiditySeconds = 604800L;
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true); // CHỈ DÙNG TRONG MÔI TRƯỜNG PRODUCTION (HTTPS)
        refreshCookie.setMaxAge((int) refreshTokenValiditySeconds);
        refreshCookie.setPath("/"); // Có thể truy cập từ mọi đường dẫn

        response.addCookie(refreshCookie);

        response.sendRedirect(
                frontendDomain + "/oauth-success"
                // Nếu bạn muốn truyền AT, hãy cân nhắc dùng cookie hoặc lưu tạm thời.
                // + "?access=" + accessToken
        );
    }
}
