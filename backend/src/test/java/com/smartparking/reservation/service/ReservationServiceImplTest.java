package com.smartparking.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.reservation.convert.ReservationConvert;
import com.smartparking.reservation.dto.CreateReservationRequest;
import com.smartparking.reservation.dto.ReservationPageQuery;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.reservation.service.impl.ReservationServiceImpl;
import com.smartparking.reservation.vo.ReservationVO;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationConvert reservationConvert;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void shouldCreateReservationAndLockParkingSpace() {
        Vehicle vehicle = vehicle(100L, 10L);
        ParkingSpace parkingSpace = parkingSpace(200L, "FREE", 0);
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(reservationConvert.toVO(any())).thenReturn(reservationVO(1L, "ACTIVE"));

        ReservationVO result = reservationService.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1)));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationMapper).insert(captor.capture());
        Reservation inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10L);
        assertThat(inserted.getVehicleId()).isEqualTo(100L);
        assertThat(inserted.getSpaceId()).isEqualTo(200L);
        assertThat(inserted.getStatus()).isEqualTo(ReservationStatus.ACTIVE.name());
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("parking:space:200"), any());
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldRejectReservationWithAnotherUsersVehicle() {
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 20L));

        assertThatThrownBy(() -> reservationService.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectWhenUserAlreadyHasActiveReservation() {
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> reservationService.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectNonFreeParkingSpace() {
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "OCCUPIED", 1));

        assertThatThrownBy(() -> reservationService.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectParkingSpaceOptimisticLockConflict() {
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "FREE", 0));
        when(parkingSpaceMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> reservationService.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1))))
                .isInstanceOf(BusinessException.class);
        verify(redisTemplate).delete("parking:space:200");
    }

    @Test
    void shouldCancelActiveReservationAndReleaseParkingSpace() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE");
        ParkingSpace parkingSpace = parkingSpace(200L, "RESERVED", 1);
        when(reservationMapper.selectById(1L)).thenReturn(reservation);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        reservationService.cancel(10L, 1L);

        assertThat(reservation.getStatus()).isEqualTo("CANCELLED");
        verify(reservationMapper).updateById(reservation);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("parking:space:200"), any());
    }

    @Test
    void shouldRejectCancellingAnotherUsersReservation() {
        when(reservationMapper.selectById(1L)).thenReturn(reservation(1L, 20L, 100L, 200L, "ACTIVE"));

        assertThatThrownBy(() -> reservationService.cancel(10L, 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPageMine() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE");
        Page<Reservation> page = Page.of(1, 10);
        page.setRecords(List.of(reservation));
        page.setTotal(1);
        when(reservationMapper.selectPage(any(), any())).thenReturn(page);
        when(reservationConvert.toVOList(List.of(reservation))).thenReturn(List.of(reservationVO(1L, "ACTIVE")));

        PageResponse<ReservationVO> result = reservationService.pageMine(10L, new ReservationPageQuery(1L, 10L, "ACTIVE", null, null, null));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
    }

    @Test
    void shouldAllowOnlyOneConcurrentReservationForSameParkingSpace() throws Exception {
        ReservationServiceImpl service = new ReservationServiceImpl(
                reservationMapper,
                reservationConvert,
                vehicleMapper,
                parkingSpaceMapper,
                redisTemplate
        );
        AtomicBoolean lockTaken = new AtomicBoolean(false);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenAnswer(invocation -> parkingSpace(200L, "FREE", 0));
        when(parkingSpaceMapper.update(any(), any())).thenAnswer(invocation -> lockTaken.compareAndSet(false, true) ? 1 : 0);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(reservationConvert.toVO(any())).thenReturn(reservationVO(1L, "ACTIVE"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Boolean> first = executor.submit(() -> createConcurrently(service, ready, start));
        Future<Boolean> second = executor.submit(() -> createConcurrently(service, ready, start));
        ready.await();
        start.countDown();

        long successes = List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count();
        executor.shutdownNow();

        assertThat(successes).isEqualTo(1);
    }

    private boolean createConcurrently(ReservationServiceImpl service, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            service.create(10L, new CreateReservationRequest(100L, 200L, LocalDateTime.now().plusHours(1)));
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private Vehicle vehicle(Long vehicleId, Long userId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setUserId(userId);
        vehicle.setPlateNumber("粤B12345");
        vehicle.setVehicleType("SMALL");
        return vehicle;
    }

    private ParkingSpace parkingSpace(Long spaceId, String status, Integer version) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setSpaceId(spaceId);
        parkingSpace.setArea("A");
        parkingSpace.setType("STANDARD");
        parkingSpace.setStatus(status);
        parkingSpace.setVersion(version);
        return parkingSpace;
    }

    private Reservation reservation(Long reservationId, Long userId, Long vehicleId, Long spaceId, String status) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setVehicleId(vehicleId);
        reservation.setSpaceId(spaceId);
        reservation.setStatus(status);
        reservation.setArriveTime(LocalDateTime.now().plusHours(1));
        reservation.setReserveTime(LocalDateTime.now());
        return reservation;
    }

    private ReservationVO reservationVO(Long reservationId, String status) {
        return new ReservationVO(reservationId, 10L, 100L, 200L, null, LocalDateTime.now().plusHours(1), status, null, null);
    }
}
