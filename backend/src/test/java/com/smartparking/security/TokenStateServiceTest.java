package com.smartparking.security;

import com.smartparking.user.entity.User;
import com.smartparking.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenStateServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TokenStateService tokenStateService;

    @Test
    void shouldDenyTokenWithRemainingTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenStateService.denyToken("jti-1", Instant.now().plusSeconds(60));

        verify(valueOperations).set(eq("auth:jwt:deny:jti-1"), eq(Boolean.TRUE), any(Duration.class));
    }

    @Test
    void shouldSkipDenyForAlreadyExpiredToken() {
        tokenStateService.denyToken("jti-1", Instant.now().minusSeconds(1));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldDetectDeniedToken() {
        when(redisTemplate.hasKey("auth:jwt:deny:jti-1")).thenReturn(true);

        assertThat(tokenStateService.isTokenDenied("jti-1")).isTrue();
    }

    @Test
    void shouldDegradeWhenRedisUnavailable() {
        when(redisTemplate.hasKey(any())).thenThrow(new RuntimeException("redis down"));

        assertThat(tokenStateService.isTokenDenied("jti-1")).isFalse();
        assertThatCode(() -> tokenStateService.denyToken("jti-1", Instant.now().plusSeconds(60)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldLoadVersionFromDbAndCacheIt() {
        User user = new User();
        user.setUserId(1L);
        user.setTokenVersion(3L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:tokenver:1")).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(user);

        long version = tokenStateService.currentVersion(1L);

        assertThat(version).isEqualTo(3L);
        verify(valueOperations).set(eq("auth:tokenver:1"), eq(3L), any(Duration.class));
    }

    @Test
    void shouldReturnCachedVersionWithoutDbAccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:tokenver:1")).thenReturn(7L);

        long version = tokenStateService.currentVersion(1L);

        assertThat(version).isEqualTo(7L);
        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldReturnMinusOneForUnknownUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:tokenver:1")).thenReturn(null);
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThat(tokenStateService.currentVersion(1L)).isEqualTo(-1L);
    }

    @Test
    void shouldEvictVersionCache() {
        tokenStateService.evictVersionCache(1L);

        verify(redisTemplate).delete("auth:tokenver:1");
    }
}
