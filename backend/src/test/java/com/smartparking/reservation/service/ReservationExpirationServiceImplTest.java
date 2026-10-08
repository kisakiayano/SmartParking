package com.smartparking.reservation.service;

import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.reservation.config.ReservationProperties;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.reservation.service.impl.ReservationExpirationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceImplTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ReservationExpirationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationExpirationServiceImpl(
                reservationMapper,
                parkingSpaceMapper,
                redisTemplate,
                new TransactionTemplate(transactionManager),
                new ReservationProperties(30, 60000, 30000)
        );
    }

    @Test
    void shouldExpireStaleReservationAndReleaseSpace() {
        Reservation stale = staleReservation(1L, 200L);
        when(reservationMapper.selectList(any())).thenReturn(List.of(stale));
        when(reservationMapper.selectById(1L)).thenReturn(stale);
        when(reservationMapper.update(any(), any())).thenReturn(1);
        ParkingSpace space = parkingSpace(200L, ParkingSpaceStatus.RESERVED.name(), 3);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(space);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        int expired = service.expireStaleReservations();

        assertThat(expired).isEqualTo(1);
        verify(valueOperations).set(eq("parking:space:200"), any());
    }

    @Test
    void shouldSkipSpaceReleaseWhenSpaceOccupied() {
        Reservation stale = staleReservation(1L, 200L);
        when(reservationMapper.selectList(any())).thenReturn(List.of(stale));
        when(reservationMapper.selectById(1L)).thenReturn(stale);
        when(reservationMapper.update(any(), any())).thenReturn(1);
        ParkingSpace space = parkingSpace(200L, ParkingSpaceStatus.OCCUPIED.name(), 5);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(space);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(0);

        int expired = service.expireStaleReservations();

        // 预约照常过期；车位已被入场占用时不回滚、不释放，仅清缓存
        assertThat(expired).isEqualTo(1);
        verify(redisTemplate).delete("parking:space:200");
        verify(valueOperations, never()).set(any(), any());
    }

    @Test
    void shouldNotExpireWhenReservationCasFails() {
        Reservation stale = staleReservation(1L, 200L);
        when(reservationMapper.selectList(any())).thenReturn(List.of(stale));
        when(reservationMapper.selectById(1L)).thenReturn(stale);
        when(reservationMapper.update(any(), any())).thenReturn(0);

        int expired = service.expireStaleReservations();

        assertThat(expired).isZero();
        verify(parkingSpaceMapper, never()).selectById(any());
    }

    @Test
    void shouldReturnZeroWhenNothingStale() {
        when(reservationMapper.selectList(any())).thenReturn(List.of());

        int expired = service.expireStaleReservations();

        assertThat(expired).isZero();
    }

    private Reservation staleReservation(Long reservationId, Long spaceId) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(reservationId);
        reservation.setUserId(10L);
        reservation.setVehicleId(100L);
        reservation.setSpaceId(spaceId);
        reservation.setStatus(ReservationStatus.ACTIVE.name());
        reservation.setArriveTime(LocalDateTime.now().minusHours(2));
        return reservation;
    }

    private ParkingSpace parkingSpace(Long spaceId, String status, Integer version) {
        ParkingSpace space = new ParkingSpace();
        space.setSpaceId(spaceId);
        space.setArea("A");
        space.setType("STANDARD");
        space.setStatus(status);
        space.setVersion(version);
        return space;
    }
}
