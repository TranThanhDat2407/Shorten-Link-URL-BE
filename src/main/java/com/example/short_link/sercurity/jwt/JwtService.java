package com.example.short_link.sercurity.jwt;

import com.example.short_link.exception.InvalidTokenException;
import com.example.short_link.util.RedisService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey signingKey;

    private final RedisService redisService;

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return signingKey;
    }

    // ==================== GENERATE TOKENS ====================

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), accessTokenExpiration, "ACCESS");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), refreshTokenExpiration, "REFRESH");
    }

    public String generatePasswordResetToken(String email) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(email)
                .claim("type", "PASSWORD_RESET")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private String buildToken(String subject, long expirationSeconds, String type) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)
                .setSubject(subject)
                .claim("type", type)
                .claim("roles", "USER") // nếu cần truyền roles/authorities
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expirationSeconds, ChronoUnit.SECONDS)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // ==================== EXTRACT CLAIMS ====================

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration().toInstant());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ==================== VALIDATION ====================

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            String type = extractType(token);
            String jti = extractJti(token);

            return email.equals(userDetails.getUsername())
                    && "ACCESS".equals(type)
                    && !isTokenExpired(token)
                    && !isTokenBlacklisted(jti); // nếu bạn dùng Redis blacklist
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            String type = extractType(token);
            String jti = extractJti(token);

            return email.equals(userDetails.getUsername())
                    && "REFRESH".equals(type)
                    && !isTokenExpired(token)
                    && !isTokenBlacklisted(jti);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (Exception e) {
            return true;
        }
    }

    // Dùng khi bạn có Redis blacklist (rất nên có!)
    private boolean isTokenBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            return redisService.isTokenBlacklisted(jti);
        } catch (Exception e) {
            log.warn("Redis error when checking blacklist for jti: {}. Assuming not blacklisted.", jti);
            return false; // Fail-open: nếu Redis down → vẫn cho phép (ưu tiên availability)
        }
    }

    // ==================== HELPER METHODS ====================

    public long getRemainingSeconds(String token) {
        try {
            Instant expiration = extractExpiration(token);
            return Math.max(0, java.time.Duration.between(Instant.now(), expiration).getSeconds());
        } catch (Exception e) {
            log.warn("Cannot calculate TTL for token", e);
            return 0;
        }
    }

    // Dành riêng cho validate password reset token (rất chặt chẽ)
    public String validatePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!"PASSWORD_RESET".equals(claims.get("type"))) {
                throw new InvalidTokenException("Invalid token type");
            }

            if (isTokenExpired(token)) {
                throw new InvalidTokenException("Password reset token has expired");
            }

            String jti = claims.getId();
            if (isTokenBlacklisted(jti)) {
                throw new InvalidTokenException("Token has already been used");
            }

            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Password reset token has expired");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid password reset token");
        }
    }
}
