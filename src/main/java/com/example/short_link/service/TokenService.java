package com.example.short_link.service;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.Optional;


public interface TokenService {

    void saveUserToken(User user, String refreshToken, Instant expiresAt, String deviceName, String ipAddress);

    @Transactional
    void revokeAllUserTokens(User user);

    Optional<Token> findByToken(String token);

    String refreshAccessToken(String refreshToken);

    @Transactional
    void revokeRefreshToken(String refreshToken);


    @Transactional
    void revokeAllUserRefreshTokens(User user);

    // Scheduler sẽ gọi method này để xóa token đã revoked và expired
    @Transactional
    void cleanupExpiredAndRevokedTokens();

    void limitTokensPerDevice(User user, String deviceType, int maxTokensPerDevice);
}
