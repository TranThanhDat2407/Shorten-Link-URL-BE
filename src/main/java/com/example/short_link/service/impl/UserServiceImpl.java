package com.example.short_link.service.impl;

import com.example.short_link.dto.request.LoginRequest;
import com.example.short_link.dto.request.RegisterRequest;
import com.example.short_link.dto.request.UserSearchRequest;
import com.example.short_link.dto.response.AuthResponse;
import com.example.short_link.entity.User;
import com.example.short_link.enums.AuthProvider;
import com.example.short_link.enums.Role;
import com.example.short_link.exception.DataNotFoundException;
import com.example.short_link.repository.UserRepository;
import com.example.short_link.repository.spec.UserSpecification;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.sercurity.user.CustomUserDetailsService;
import com.example.short_link.service.TokenService;
import com.example.short_link.service.UserService;
import com.example.short_link.util.RedisService;
import com.example.short_link.util.UserAgentParsingUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final UserAgentParsingUtil userAgentUtil;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    @Override
    public User register(RegisterRequest request) {
        Optional<User> optinalUser = userRepository.findByEmail(request.getEmail());

        //tìm xem user co ton tai khong
        if (optinalUser.isPresent()) {
            // tồn tại thì xem có phải ADMIN không
            if (optinalUser.get().getRole().equals(Role.ADMIN)) {
                throw new PermissionDenyException("Cannot create admin account");
            }

            throw new DataNotFoundException("User already Exists");
        }

        // tìm index của @
        int indexAt = request.getEmail().indexOf('@');

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                //fullname = subtring trước @
                .fullName(request.getEmail().substring(0, indexAt))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .isActive(true)
                .build();

        userRepository.save(user);
        return user;
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new DataNotFoundException("User Not Found");
        }

        User existingUser = userOptional.get();

        if (existingUser.getProvider() == AuthProvider.GOOGLE) {
            throw new RuntimeException("This account was registered via Google. " +
                    "Use Google Login instead.");
        }

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (!existingUser.isActive()) {
            throw new BadCredentialsException("User is locked");
        }


        UserDetails userDetails = customUserDetailsService.loadUserByUsername(existingUser.getEmail());

        //TẠO ACCESS TOKEN
        String accessToken = jwtService.generateAccessToken(userDetails);

        // 6. Tạo refresh token mới
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Instant expiresAt = jwtService.getRefreshTokenExpirationInstant();


        // 7. Lấy device + IP
        String userAgentHeader = httpRequest.getHeader("User-Agent");
        String deviceName = userAgentUtil.getDevice(userAgentHeader)
                + " • " + userAgentUtil.getBrowser(userAgentHeader);
        String ipAddress = httpRequest.getRemoteAddr();

        //giới hạn token mỗi device
        tokenService.limitTokensPerDevice(existingUser, deviceName, 2);

        // 8. Lưu refresh token kèm device info
        tokenService.saveUserToken(existingUser, refreshToken, expiresAt, deviceName, ipAddress);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("Invalid user"));
    }


    @Override
    public Page<User> searchUsers(UserSearchRequest request, Pageable pageable) {
        Specification<User> spec = Specification.unrestricted();

        if (request.getEmail() != null) {
            spec = spec.and(UserSpecification.containsEmail(request.getEmail()));
        }

        if (request.getFullName() != null) {
            spec = spec.and(UserSpecification.containsFullName(request.getFullName()));
        }

        if (request.getProvider() != null) {
            spec = spec.and(UserSpecification.hasProvider(request.getProvider()));
        }

        if (request.getIsActive() != null) {
            spec = spec.and(UserSpecification.isActive(request.getIsActive()));
        }

        return userRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        // 1. Blacklist Access Token (nếu có)
        if (StringUtils.hasText(accessToken)) {
            long remainingSeconds = jwtService.getAccessTokenRemainingSeconds(accessToken);
            if (remainingSeconds > 0) {
                redisService.blacklistAccessToken(accessToken, remainingSeconds);
            }
        }

        // 2. Blacklist + Xóa Refresh Token (nếu có)
        if (StringUtils.hasText(refreshToken)) {
            // revoke
            tokenService.revokeRefreshToken(refreshToken);

            // Blacklist trong Redis (nếu hệ thống có nhiều instance)
            long refreshRemaining = jwtService.getRefreshTokenRemainingSeconds(refreshToken);
            if (refreshRemaining > 0) {
                redisService.blacklistRefreshToken(refreshToken, refreshRemaining);
            }

        }

        // xóa trong cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        // Optional: Clear SecurityContext (dù stateless nhưng vẫn nên)
        SecurityContextHolder.clearContext();
    }
}
