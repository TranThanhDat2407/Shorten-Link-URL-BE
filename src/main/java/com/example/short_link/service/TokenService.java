package com.example.short_link.service;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;

import java.time.Instant;
import java.util.Optional;

public interface TokenService {
    void saveUserToken(User user, String refreshToken, Instant expiresAt);

    void revokeAllUserTokens(User user);

    Optional<Token> findByToken(String token);

    String refreshAccessToken(String refreshToken);
}
