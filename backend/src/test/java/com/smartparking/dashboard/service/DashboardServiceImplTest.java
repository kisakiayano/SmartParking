package com.smartparking.dashboard.service;

import com.smartparking.dashboard.service.impl.DashboardServiceImpl;
import com.smartparking.dashboard.vo.DashboardStatisticsVO;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.mapper.ParkingRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ParkingRecordMapper parkingRecordMapper;

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void shouldReturnCachedStatistics() {
        DashboardStatisticsVO cached = statistics();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:dashboard:statistics")).thenReturn(cached);

        DashboardStatisticsVO result = dashboardService.getStatistics();

        assertThat(result).isSameAs(cached);
        verify(parkingSpaceMapper, never()).selectCount(any());
        verify(parkingRecordMapper, never()).selectList(any());
    }

    @Test
    void shouldCalculateStatisticsAndCacheForFiveMinutes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:dashboard:statistics")).thenReturn(null);
        when(parkingSpaceMapper.selectCount(any()))
                .thenReturn(10L)
                .thenReturn(5L)
                .thenReturn(2L)
                .thenReturn(3L);
        when(parkingRecordMapper.selectList(any())).thenReturn(List.of(record("5.00"), record("7.50")));
        when(parkingRecordMapper.selectCount(any())).thenReturn(2L);

        DashboardStatisticsVO result = dashboardService.getStatistics();

        assertThat(result.todayIncome()).isEqualByComparingTo("12.50");
        assertThat(result.todayParkingCount()).isEqualTo(2L);
        assertThat(result.totalSpaces()).isEqualTo(10L);
        assertThat(result.freeSpaces()).isEqualTo(5L);
        assertThat(result.reservedSpaces()).isEqualTo(2L);
        assertThat(result.occupiedSpaces()).isEqualTo(3L);
        assertThat(result.currentOccupancyRate()).isEqualByComparingTo("30.00");
        assertThat(result.revenueTrend7Days()).hasSize(7);
        assertThat(result.revenueTrend30Days()).hasSize(30);
        assertThat(result.trafficTrend7Days()).hasSize(7);
        assertThat(result.trafficTrend30Days()).hasSize(30);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("parking:dashboard:statistics"), eq(result), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldUseZeroOccupancyWhenNoParkingSpaces() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:dashboard:statistics")).thenReturn(null);
        when(parkingSpaceMapper.selectCount(any()))
                .thenReturn(0L)
                .thenReturn(0L)
                .thenReturn(0L)
                .thenReturn(0L);
        when(parkingRecordMapper.selectList(any())).thenReturn(List.of());
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);

        DashboardStatisticsVO result = dashboardService.getStatistics();

        assertThat(result.currentOccupancyRate()).isEqualByComparingTo("0.00");
    }

    private ParkingRecord record(String fee) {
        ParkingRecord record = new ParkingRecord();
        record.setFee(new BigDecimal(fee));
        return record;
    }

    private DashboardStatisticsVO statistics() {
        return new DashboardStatisticsVO(
                new BigDecimal("10.00"),
                2L,
                10L,
                5L,
                2L,
                3L,
                new BigDecimal("30.00"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
