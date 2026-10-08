package com.smartparking.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartparking.dashboard.service.DashboardService;
import com.smartparking.dashboard.vo.DashboardStatisticsVO;
import com.smartparking.dashboard.vo.TrendPointVO;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.enums.ParkingRecordStatus;
import com.smartparking.record.mapper.ParkingRecordMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_KEY = "parking:dashboard:statistics";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final ParkingRecordMapper parkingRecordMapper;
    private final ParkingSpaceMapper parkingSpaceMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public DashboardServiceImpl(
            ParkingRecordMapper parkingRecordMapper,
            ParkingSpaceMapper parkingSpaceMapper,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.parkingRecordMapper = parkingRecordMapper;
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public DashboardStatisticsVO getStatistics() {
        DashboardStatisticsVO cached = getCachedStatistics();
        if (cached != null) {
            return cached;
        }
        DashboardStatisticsVO statistics = calculateStatistics();
        cacheStatistics(statistics);
        return statistics;
    }

    private DashboardStatisticsVO calculateStatistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Long totalSpaces = parkingSpaceMapper.selectCount(new LambdaQueryWrapper<>());
        Long freeSpaces = countSpaces(ParkingSpaceStatus.FREE);
        Long reservedSpaces = countSpaces(ParkingSpaceStatus.RESERVED);
        Long occupiedSpaces = countSpaces(ParkingSpaceStatus.OCCUPIED);

        return new DashboardStatisticsVO(
                sumIncome(todayStart, tomorrowStart),
                countTraffic(todayStart, tomorrowStart),
                totalSpaces,
                freeSpaces,
                reservedSpaces,
                occupiedSpaces,
                calculateOccupancyRate(occupiedSpaces, totalSpaces),
                buildRevenueTrend(today, 7),
                buildRevenueTrend(today, 30),
                buildTrafficTrend(today, 7),
                buildTrafficTrend(today, 30)
        );
    }

    private Long countSpaces(ParkingSpaceStatus status) {
        return parkingSpaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getStatus, status.name()));
    }

    private BigDecimal sumIncome(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        List<ParkingRecord> records = parkingRecordMapper.selectList(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getStatus, ParkingRecordStatus.FINISHED.name())
                .ge(ParkingRecord::getExitTime, startInclusive)
                .lt(ParkingRecord::getExitTime, endExclusive));
        return records.stream()
                .map(ParkingRecord::getFee)
                .filter(fee -> fee != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Long countTraffic(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return parkingRecordMapper.selectCount(new LambdaQueryWrapper<ParkingRecord>()
                .ge(ParkingRecord::getEntryTime, startInclusive)
                .lt(ParkingRecord::getEntryTime, endExclusive));
    }

    private BigDecimal calculateOccupancyRate(Long occupiedSpaces, Long totalSpaces) {
        if (totalSpaces == null || totalSpaces == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long occupied = occupiedSpaces == null ? 0L : occupiedSpaces;
        return BigDecimal.valueOf(occupied)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSpaces), 2, RoundingMode.HALF_UP);
    }

    private List<TrendPointVO> buildRevenueTrend(LocalDate today, int days) {
        List<TrendPointVO> trend = new ArrayList<>();
        LocalDate startDate = today.minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            trend.add(new TrendPointVO(date, sumIncome(date.atStartOfDay(), date.plusDays(1).atStartOfDay())));
        }
        return trend;
    }

    private List<TrendPointVO> buildTrafficTrend(LocalDate today, int days) {
        List<TrendPointVO> trend = new ArrayList<>();
        LocalDate startDate = today.minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            trend.add(new TrendPointVO(
                    date,
                    BigDecimal.valueOf(countTraffic(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
            ));
        }
        return trend;
    }

    private DashboardStatisticsVO getCachedStatistics() {
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached instanceof DashboardStatisticsVO statistics) {
                return statistics;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private void cacheStatistics(DashboardStatisticsVO statistics) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, statistics, CACHE_TTL);
        } catch (RuntimeException ignored) {
            // Dashboard can be recalculated from MySQL when Redis is unavailable.
        }
    }
}
