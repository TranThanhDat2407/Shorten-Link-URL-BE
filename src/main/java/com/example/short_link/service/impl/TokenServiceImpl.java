package com.example.short_link.service.impl;

import com.example.short_link.entity.Token;
import com.example.short_link.entity.User;
import com.example.short_link.enums.TokenType;
import com.example.short_link.repository.TokenRepository;
import com.example.short_link.sercurity.jwt.JwtService;
import com.example.short_link.service.TokenService;
import lombok.RequiredArgsConstructor;
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
    private final JwtService jwtService;

    @Override
    public void saveUserToken(User user, String refreshToken, Instant expiresAt) {
        Token token = Token.builder()
                .token(refreshToken)
                .user(user)
                .tokenType(TokenType.REFRESH)
                .revoked(false)
                .expired(false)
                .expiredAt(expiresAt)
                .build();

        tokenRepository.save(token);
    }

    @Override
    public void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenRepository.findAllByUserAndRevokedFalseAndExpiredFalse(user);
        if (!validTokens.isEmpty()) {
            validTokens.forEach(t -> {
                t.setRevoked(true);
                t.setExpired(true);
            });
            tokenRepository.saveAll(validTokens);
        }
    }

    @Override
    public Optional<Token> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        // Tìm refresh token trong DB
        Optional<Token> storedTokenOpt = tokenRepository.findByToken(refreshToken);

        if (storedTokenOpt.isEmpty()
                || storedTokenOpt.get().getRevoked()
                || storedTokenOpt.get().getExpired()) {
            throw new RuntimeException("Refresh token is invalid or expired.");
        }

         Token storedToken = storedTokenOpt.get();

        // Load userDetails
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        storedToken.getUser().getEmail()
                );

        // Kiểm tra refresh token còn hợp lệ
        if (!jwtService.isValidateToken(refreshToken, userDetails)) {

            // Nếu token hết hạn → set expired trong DB
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            tokenRepository.save(storedToken);

            throw new RuntimeException("Refresh token is expired. Please login again.");
        }

        // Sinh Access Token mới
        return jwtService.generateAccessToken(userDetails);
    }
}
