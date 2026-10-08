package com.smartparking.security;

import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window login failure counter in Redis: after MAX_FAILURES failures the
 * username is locked for LOCK_DURATION. Redis failures degrade to "allow".
 */
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "auth:login:fail:";
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;

    public LoginRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAllowed(String username) {
        try {
            Object failures = redisTemplate.opsForValue().get(KEY_PREFIX + username);
            if (failures instanceof Number number && number.longValue() >= MAX_FAILURES) {
                throw new BusinessException(ResultCode.TOO_MANY_REQUESTS,
                        "too many failed login attempts, please try again later");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ignored) {
            // Redis unavailable: do not block logins because of the limiter.
        }
    }

    public void onFailure(String username) {
        try {
            Long failures = redisTemplate.opsForValue().increment(KEY_PREFIX + username);
            if (failures != null && failures == 1L) {
                redisTemplate.expire(KEY_PREFIX + username, LOCK_DURATION);
            }
        } catch (RuntimeException ignored) {
            // Redis unavailable: brute-force protection degrades gracefully.
        }
    }

    public void onSuccess(String username) {
        try {
            redisTemplate.delete(KEY_PREFIX + username);
        } catch (RuntimeException ignored) {
            // Counter expiry via TTL handles this case anyway.
        }
    }
}
