package com.example.short_link.util;


import com.example.short_link.repository.LinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final LinkRepository shortLinkRepository;

    private static final String OTP_PREFIX = "otp:";
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    public static final String CACHE_SHORT = "sl:code:";     // sl:code:abc123 → originalUrl
    private static final String CLICK_TEMP  = "sl:click:";// sl:click:abc123 → temporary click count
    private static final String CLICK_TEMP_KEY_PATTERN = CLICK_TEMP + "*";

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

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            // Deserialize JSON String thành Object T
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON from Redis", e);
        }
    }

    public void set(String key, Object object, Duration ttl) {
        try {
            //Serialize Object thành JSON String
            String json = objectMapper.writeValueAsString(object);
            // Lưu JSON String vào Redis
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object to JSON for Redis", e);
        }
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

    public void logTemporaryClick(String shortCode, Duration ttl) {
        String key = CLICK_TEMP + shortCode;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, ttl);
    }

    @Scheduled(cron = "0 0/5 * * * *") // 5 phút
    @Transactional
    public void syncClicksFromRedisToDb() {
        // Tìm tất cả các key click count tạm thời trong Redis
        Set<String> keys = redisTemplate.keys(CLICK_TEMP_KEY_PATTERN);

        if (keys == null || keys.isEmpty()) {
            return;
        }


        for (String key : keys) {
            try {
                // Lấy click count tạm thời
                String countStr = redisTemplate.opsForValue().get(key);
                // Xóa key ngay lập tức để tránh đọc lại hoặc bỏ sót click mới
                redisTemplate.delete(key);

                if (countStr == null) continue;

                Long incrementCount = Long.parseLong(countStr);

                // Trích xuất shortCode từ key
                String shortCode = key.substring(CLICK_TEMP.length());

                // Tìm Link trong DB
                shortLinkRepository.findByShortCode(shortCode).ifPresent(link -> {
                    // Cập nhật và lưu vào DB
                    link.setClickCount(link.getClickCount() + incrementCount);
                    shortLinkRepository.save(link);
                });

            } catch (Exception e) {
                System.err.println( key + ": " + e.getMessage());
            }
        }
    }

}
