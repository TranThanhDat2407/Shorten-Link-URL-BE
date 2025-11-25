package com.example.short_link.util;


import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    private static final String CACHE_SHORT = "sl:code:";     // sl:code:abc123 → originalUrl
    private static final String CACHE_ID    = "sl:id:";       // sl:id:12345 → full Link JSON
    private static final String CLICK_TEMP  = "sl:click:";    // sl:click:abc123 → temporary click count

    public void saveOtp(String email, String otp, Duration ttl) {
        redisTemplate.opsForValue()
                .set(OTP_PREFIX + email, otp, ttl);
    }

    public String getOtpAndRemove(String email) {
        String key = OTP_PREFIX + email;
        String otp = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return otp;
    }

    public long incrementAndExpire(String key, long value, Duration ttl) {
        Long result = redisTemplate.opsForValue().increment(key, value);
        if (result != null && result == value) { // lần đầu tạo key
            redisTemplate.expire(key, ttl);
        }
        return result != null ? result : 0;
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    public void blacklistToken(String jti, long seconds) {
        redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(seconds));
    }

    public boolean isTokenBlacklisted(String jti) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
    }

}
