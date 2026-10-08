package com.smartparking.security;

import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    private static final String KEY = "auth:login:fail:user01";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LoginRateLimiter loginRateLimiter;

    @Test
    void shouldAllowWhenUnderThreshold() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(4L);

        assertThatCode(() -> loginRateLimiter.checkAllowed("user01")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenLocked() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(5L);

        assertThatThrownBy(() -> loginRateLimiter.checkAllowed("user01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getResultCode()).isEqualTo(ResultCode.TOO_MANY_REQUESTS));
    }

    @Test
    void shouldRecordFailureAndSetExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(1L);

        loginRateLimiter.onFailure("user01");

        verify(redisTemplate).expire(eq(KEY), any(Duration.class));
    }

    @Test
    void shouldClearCounterOnSuccess() {
        loginRateLimiter.onSuccess("user01");

        verify(redisTemplate).delete(KEY);
    }

    @Test
    void shouldDegradeWhenRedisUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThatCode(() -> {
            loginRateLimiter.checkAllowed("user01");
            loginRateLimiter.onFailure("user01");
            loginRateLimiter.onSuccess("user01");
        }).doesNotThrowAnyException();
    }
}
