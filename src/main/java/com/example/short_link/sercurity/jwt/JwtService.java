package com.example.short_link.sercurity.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${jwt.expiration}")
    private int expiration;

    @Value("${jwt.expiration-refresh-token}")
    private int expirationRefreshToken;

    @Value("${jwt.secretKey}")
    private String secretKey;

    public String generateAccessToken(UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                // thời điểm hiện tại + thời gian sống = thời điểm hết han
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                // thời điểm hiện tại + thời gian sống = thời điểm hết han
                .setExpiration(new Date(System.currentTimeMillis() + expirationRefreshToken * 1000L))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        //Giải mã secretKey từ Base64 bằng Decoders.BASE64.decode().
        byte[] bytes = Decoders.BASE64.decode(secretKey);

        //Tạo một đối tượng Key sử dụng Keys.hmacShaKeyFor().
        return Keys.hmacShaKeyFor(bytes);
    }

    // Trích xuất tất cả các claims (dữ liệu tùy chỉnh) từ JWT.
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // Trích xuất một claim cụ thể từ JWT.
    public  <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    // Kiểm tra token có hết hạn hay không.
    public boolean isTokenExpired(String token) {
        Date expirationDate = this.extractClaim(token, Claims::getExpiration);
        return expirationDate.before(new Date());
    }
    //Trích Subject(Email) từ token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //Xác minh JWT có hợp lệ không.
    public boolean isValidateToken(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()))
                && !isTokenExpired(token);
    }

    // Lấy thời điểm hết hạn refresh token
    public Instant getRefreshTokenExpirationInstant() {
        return Instant.now().plusMillis(expirationRefreshToken * 1000L);
    }

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

   // lấy thời gian sống để đưa vào redis
    public long getAccessTokenRemainingSeconds(String accessToken) {
        try {
            Date expiration = extractClaim(accessToken, Claims::getExpiration);
            long expirationMillis = expiration.getTime();
            long nowMillis = System.currentTimeMillis();
            return Math.max(0, (expirationMillis - nowMillis) / 1000); // trả về giây, không âm
        } catch (Exception e) {
            log.warn("Cannot parse access token for blacklist calculation: {}", e.getMessage());
            return 0;
        }
    }

    // lấy thời gian sống để đưa vào redis
    public long getRefreshTokenRemainingSeconds(String refreshToken) {
        try {
            Date expiration = extractClaim(refreshToken, Claims::getExpiration);
            long expirationMillis = expiration.getTime();
            long nowMillis = System.currentTimeMillis();
            return Math.max(0, (expirationMillis - nowMillis) / 1000);
        } catch (Exception e) {
            log.warn("Cannot parse refresh token for blacklist calculation: {}", e.getMessage());
            return 0;
        }
    }
}
