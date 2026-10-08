package com.smartparking.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.reservation.config.ReservationProperties;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.reservation.service.ReservationExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpirationServiceImpl implements ReservationExpirationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationServiceImpl.class);

    private static final String PARKING_SPACE_CACHE_PREFIX = "parking:space:";
    private static final int BATCH_LIMIT = 200;

    private final ReservationMapper reservationMapper;
    private final ParkingSpaceMapper parkingSpaceMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ReservationProperties properties;

    public ReservationExpirationServiceImpl(
            ReservationMapper reservationMapper,
            ParkingSpaceMapper parkingSpaceMapper,
            RedisTemplate<String, Object> redisTemplate,
            TransactionTemplate transactionTemplate,
            ReservationProperties properties
    ) {
        this.reservationMapper = reservationMapper;
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    @Override
    public int expireStaleReservations() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(properties.expirationGraceMinutes());
        List<Reservation> staleReservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStatus, ReservationStatus.ACTIVE.name())
                .lt(Reservation::getArriveTime, deadline)
                .orderByAsc(Reservation::getArriveTime)
                .last("LIMIT " + BATCH_LIMIT));
        int expired = 0;
        for (Reservation stale : staleReservations) {
            Boolean done = transactionTemplate.execute(txStatus -> expireOne(stale.getReservationId()));
            if (Boolean.TRUE.equals(done)) {
                expired++;
            }
        }
        return expired;
    }

    /**
     * 单条过期处理：预约状态 CAS（ACTIVE→EXPIRED）与用户入场并发时天然互斥，
     * 一方 CAS 失败即让位；车位已被占用（OCCUPIED）时仅跳过释放，不影响预约过期。
     */
    private boolean expireOne(Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null || !ReservationStatus.ACTIVE.name().equals(reservation.getStatus())) {
            return false;
        }
        int updated = reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("reservation_id", reservationId)
                .eq("status", ReservationStatus.ACTIVE.name())
                .set("status", ReservationStatus.EXPIRED.name())
                .set("update_time", LocalDateTime.now()));
        if (updated == 0) {
            return false;
        }
        releaseSpaceQuietly(reservation.getSpaceId());
        return true;
    }

    private void releaseSpaceQuietly(Long spaceId) {
        ParkingSpace space = parkingSpaceMapper.selectById(spaceId);
        if (space == null) {
            return;
        }
        int released = parkingSpaceMapper.update(null, new UpdateWrapper<ParkingSpace>()
                .eq("space_id", spaceId)
                .eq("status", ParkingSpaceStatus.RESERVED.name())
                .eq("version", space.getVersion())
                .set("status", ParkingSpaceStatus.FREE.name())
                .set("update_time", LocalDateTime.now())
                .setSql("version = version + 1"));
        if (released > 0) {
            cacheSpaceStatus(new ParkingSpaceStatusVO(
                    space.getSpaceId(),
                    space.getArea(),
                    space.getType(),
                    ParkingSpaceStatus.FREE.name(),
                    space.getVersion() + 1
            ));
        } else {
            log.info("parking space {} is no longer RESERVED, skip releasing for expired reservation", spaceId);
            evictSpaceStatus(spaceId);
        }
    }

    private void cacheSpaceStatus(ParkingSpaceStatusVO statusVO) {
        try {
            redisTemplate.opsForValue().set(PARKING_SPACE_CACHE_PREFIX + statusVO.spaceId(), statusVO);
        } catch (RuntimeException ignored) {
            // Redis 失败不影响数据库状态流转，缓存由读路径回填。
        }
    }

    private void evictSpaceStatus(Long spaceId) {
        try {
            redisTemplate.delete(PARKING_SPACE_CACHE_PREFIX + spaceId);
        } catch (RuntimeException ignored) {
            // Redis 失败不隐藏业务结果。
        }
    }
}
