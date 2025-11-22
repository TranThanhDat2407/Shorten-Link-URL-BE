package com.example.short_link.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLISTED_VALUE = "1";

    public void set(String key, String value, long timeout, TimeUnit timeUnit){
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public String get(String key){
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key){
        redisTemplate.delete(key);
    }

    // Blacklist access token
    public void blacklistAccessToken(String token, long seconds) {
        if (seconds <= 0) return;
        redisTemplate.opsForValue()
                .set("blacklist:access:" + token, BLACKLISTED_VALUE, seconds, TimeUnit.SECONDS);
    }

    // Blacklist refresh token
    public void blacklistRefreshToken(String token, long seconds) {
        if (seconds <= 0) return;
        redisTemplate.opsForValue()
                .set("blacklist:refresh:" + token, BLACKLISTED_VALUE, seconds, TimeUnit.SECONDS);
    }

    // Check
    public boolean isTokenBlacklisted(String prefix, String token) {
        return redisTemplate.hasKey(prefix + token);
    }

    public boolean isAccessTokenBlacklisted(String token) {
        return isTokenBlacklisted("blacklist:access:", token);
    }

    public boolean isRefreshTokenBlacklisted(String token) {
        return isTokenBlacklisted("blacklist:refresh:", token);
    }

}
