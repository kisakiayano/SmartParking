package com.smartparking.security;

import com.smartparking.user.entity.User;
import com.smartparking.user.mapper.UserMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed token lifecycle state: revoked-token blacklist and per-user token version
 * used to invalidate sessions after password change or admin disable.
 */
@Service
public class TokenStateService {

    private static final String DENY_KEY_PREFIX = "auth:jwt:deny:";
    private static final String VERSION_KEY_PREFIX = "auth:tokenver:";
    private static final Duration VERSION_CACHE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;

    public TokenStateService(RedisTemplate<String, Object> redisTemplate, UserMapper userMapper) {
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    public void denyToken(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(DENY_KEY_PREFIX + tokenId, Boolean.TRUE, ttl);
        } catch (RuntimeException ignored) {
            // Redis unavailable: revocation degrades and the token simply lives out its TTL.
        }
    }

    public boolean isTokenDenied(String tokenId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(DENY_KEY_PREFIX + tokenId));
        } catch (RuntimeException ignored) {
            // Availability first: a broken Redis must not lock every authenticated request out.
            return false;
        }
    }

    public long currentVersion(Long userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(VERSION_KEY_PREFIX + userId);
            if (cached instanceof Number number) {
                return number.longValue();
            }
        } catch (RuntimeException ignored) {
            // Fall through to the database.
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return -1L;
        }
        long version = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        try {
            redisTemplate.opsForValue().set(VERSION_KEY_PREFIX + userId, version, VERSION_CACHE_TTL);
        } catch (RuntimeException ignored) {
            // Cache write failure is harmless; the next request reads the database again.
        }
        return version;
    }

    public void evictVersionCache(Long userId) {
        try {
            redisTemplate.delete(VERSION_KEY_PREFIX + userId);
        } catch (RuntimeException ignored) {
            // Nothing to do: the cached copy expires via TTL anyway.
        }
    }
}
