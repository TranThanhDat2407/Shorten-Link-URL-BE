package com.example.short_link.service.impl;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import com.example.short_link.enums.TokenType;
import com.example.short_link.exception.RefreshTokenRevokedException;
import com.example.short_link.repository.TokenRepository;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.service.TokenService;
import com.example.short_link.util.RedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final TokenRepository tokenRepository;
    private final UserDetailsService userDetailsService;
    private final RedisService redisService;
    private final JwtService jwtService;

    @Override
    public void saveUserToken(User user, String refreshToken,
                              Instant expiresAt, String deviceName, String ipAddress) {
        Token token = Token.builder()
                .token(refreshToken)
                .user(user)
                .tokenType(TokenType.REFRESH)  // Có thể bỏ nếu bảng chỉ lưu refresh token
                .revoked(false)
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .expiredAt(expiresAt)
                .build();

        tokenRepository.save(token);
    }

    @Transactional
    @Override
    public void revokeAllUserTokens(User user) {
        // Set revoked = true cho tất cả token chưa bị revoked
        List<Token> activeTokens = tokenRepository.findAllByUserAndRevokedFalse(user);
        if (!activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            tokenRepository.saveAll(activeTokens);

            // Đồng thời đưa vào Redis blacklist
            activeTokens.forEach(t -> {
                String jti = jwtService.extractJti(t.getToken());
                long ttl = jwtService.getRemainingSeconds(t.getToken());
                if (ttl > 0) {
                    redisService.blacklistToken(jti, ttl);
                }
            });
        }
    }

    @Override
    public Optional<Token> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        // 1. Check Redis blacklist
        if (redisService.isTokenBlacklisted(refreshToken)) {
            throw new RefreshTokenRevokedException("Refresh token has been revoked. Please login again.");
        }

        // 2. Check DB
        Token storedToken = tokenRepository.findByToken(refreshToken)
                .filter(t -> !t.getRevoked() && t.getExpiredAt().isAfter(Instant.now()))
                .orElseThrow(() -> new RefreshTokenRevokedException("Refresh token is invalid or expired."));

        UserDetails userDetails = userDetailsService.loadUserByUsername(storedToken.getUser().getEmail());

        // 3. Validate chữ ký + thời gian
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            storedToken.setRevoked(true);
            tokenRepository.save(storedToken);
            redisService.blacklistToken(refreshToken, 0); // blacklist ngay lập tức
            throw new RefreshTokenRevokedException("Refresh token is expired. Please login again.");
        }

        // 4. Tạo access token mới
        return jwtService.generateAccessToken(userDetails);
    }

    @Transactional
    @Override
    public void revokeRefreshToken(String refreshToken) {
        tokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            tokenRepository.save(token);

            long ttl = jwtService.getRemainingSeconds(refreshToken);
            String jti = jwtService.extractJti(token.getToken());
            if (ttl > 0) {
                redisService.blacklistToken(jti, ttl);
            }
        });
    }

    @Transactional
    @Override
    public void revokeAllUserRefreshTokens(User user) {
        List<Token> allTokens = tokenRepository.findAllByUser(user);
        if (allTokens.isEmpty()) return;


        allTokens.forEach(t -> {
            t.setRevoked(true);
            String jti = jwtService.extractJti(t.getToken());
            long ttl = jwtService.getRemainingSeconds(t.getToken());
            if (ttl > 0) {
                redisService.blacklistToken(jti, ttl);
            }
        });

        tokenRepository.saveAll(allTokens);
    }

    // Scheduler sẽ gọi method này để xóa token đã revoked và expired
    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // 2h sáng mỗi ngày
    @Override
    public void cleanupExpiredAndRevokedTokens() {
        tokenRepository.deleteAllRevokedAndExpired();
    }

    @Override
    public void limitTokensPerDevice(User user, String deviceType, int maxTokensPerDevice) {
        List<Token> tokens = tokenRepository.findAllActiveTokensByUserAndDeviceType(user, deviceType);

        // Nếu vượt quá số lượng cho phép → revoke token cũ nhất
        if (tokens.size() >= maxTokensPerDevice) {
            int numToRevoke = tokens.size() - maxTokensPerDevice + 1; // +1 để nhường chỗ token mới

            for (int i = 0; i < numToRevoke; i++) {
                Token token = tokens.get(i);
                token.setRevoked(true);
                String jti = jwtService.extractJti(token.getToken());
                // Blacklist trong Redis
                long ttl = jwtService.getRemainingSeconds(token.getToken());
                if (ttl > 0) {
                    redisService.blacklistToken(jti, ttl);
                }
            }

            tokenRepository.saveAll(tokens.subList(0, numToRevoke));
        }
    }

}
