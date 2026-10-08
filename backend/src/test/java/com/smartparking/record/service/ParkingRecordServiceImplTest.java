package com.smartparking.record.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.fee.service.FeeCalculatorService;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.record.convert.ParkingRecordConvert;
import com.smartparking.record.dto.ManualEntryRequest;
import com.smartparking.record.dto.ManualExitRequest;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.enums.ParkingRecordStatus;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.record.service.impl.ParkingRecordServiceImpl;
import com.smartparking.record.vo.ParkingRecordVO;
import com.smartparking.reservation.config.ReservationProperties;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingRecordServiceImplTest {

    @Mock
    private ParkingRecordMapper parkingRecordMapper;

    @Mock
    private ParkingRecordConvert parkingRecordConvert;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private FeeCalculatorService feeCalculatorService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ParkingRecordServiceImpl parkingRecordService;

    @BeforeEach
    void setUp() {
        parkingRecordService = new ParkingRecordServiceImpl(
                parkingRecordMapper,
                parkingRecordConvert,
                vehicleMapper,
                parkingSpaceMapper,
                reservationMapper,
                feeCalculatorService,
                new ReservationProperties(30, 60000, 30000),
                redisTemplate
        );
    }

    @Test
    void shouldCreateParkingRecordOnManualEntry() {
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "FREE", 0));
        when(reservationMapper.selectOne(any())).thenReturn(null);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingRecordConvert.toVO(any())).thenReturn(recordVO(1L, "PARKING"));

        ParkingRecordVO result = parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L));

        ArgumentCaptor<ParkingRecord> captor = ArgumentCaptor.forClass(ParkingRecord.class);
        verify(parkingRecordMapper).insert(captor.capture());
        ParkingRecord inserted = captor.getValue();
        assertThat(inserted.getVehicleId()).isEqualTo(100L);
        assertThat(inserted.getSpaceId()).isEqualTo(200L);
        assertThat(inserted.getStatus()).isEqualTo(ParkingRecordStatus.PARKING.name());
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("parking:space:200"), any());
        assertThat(result.status()).isEqualTo("PARKING");
    }

    @Test
    void shouldUseActiveReservationOnManualEntry() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE", LocalDateTime.now().plusHours(1));
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "RESERVED", 1));
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(reservationMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingRecordConvert.toVO(any())).thenReturn(recordVO(1L, "PARKING"));

        parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L));

        verify(reservationMapper).update(any(), any());
    }

    @Test
    void shouldRejectEntryWithExpiredReservation() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE", LocalDateTime.now().minusHours(2));
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "RESERVED", 1));
        when(reservationMapper.selectOne(any())).thenReturn(reservation);

        assertThatThrownBy(() -> parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectEntryWhenReservationNoLongerActive() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE", LocalDateTime.now().plusHours(1));
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "RESERVED", 1));
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(reservationMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldAutoEntryWithActiveReservationSpace() {
        Reservation reservation = reservation(1L, 10L, 100L, 200L, "ACTIVE", LocalDateTime.now().plusHours(1));
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "RESERVED", 1));
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(reservationMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingRecordConvert.toVO(any())).thenReturn(recordVO(1L, "PARKING"));

        ParkingRecordVO result = parkingRecordService.autoEntry("粤B12345", null);

        assertThat(result.status()).isEqualTo("PARKING");
    }

    @Test
    void shouldAutoEntryWithFirstFreeSpaceWhenReservationMissing() {
        ParkingSpace freeSpace = parkingSpace(200L, "FREE", 0);
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(reservationMapper.selectOne(any())).thenReturn(null);
        when(parkingSpaceMapper.selectOne(any())).thenReturn(freeSpace);
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(freeSpace);
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingRecordConvert.toVO(any())).thenReturn(recordVO(1L, "PARKING"));

        ParkingRecordVO result = parkingRecordService.autoEntry("粤B12345", null);

        assertThat(result.status()).isEqualTo("PARKING");
    }

    @Test
    void shouldRejectDuplicateEntry() {
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectEntryWhenParkingSpaceOccupied() {
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "OCCUPIED", 2));
        when(reservationMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> parkingRecordService.manualEntry(new ManualEntryRequest("粤B12345", 200L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFinishParkingRecordOnManualExit() {
        ParkingRecord record = parkingRecord(1L, 100L, 200L, "PARKING");
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectOne(any())).thenReturn(record);
        when(parkingSpaceMapper.selectById(200L)).thenReturn(parkingSpace(200L, "OCCUPIED", 2));
        when(feeCalculatorService.calculate(any(), any())).thenReturn(new BigDecimal("5.00"));
        when(parkingSpaceMapper.update(any(), any())).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingRecordConvert.toVO(record)).thenReturn(recordVO(1L, "FINISHED"));

        ParkingRecordVO result = parkingRecordService.manualExit(new ManualExitRequest("粤B12345"));

        assertThat(record.getStatus()).isEqualTo("FINISHED");
        assertThat(record.getExitTime()).isNotNull();
        assertThat(record.getDuration()).isPositive();
        assertThat(record.getFee()).isEqualByComparingTo("5.00");
        verify(parkingRecordMapper).updateById(record);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("parking:space:200"), any());
        assertThat(result.status()).isEqualTo("FINISHED");
    }

    @Test
    void shouldRejectExitWithoutParkingRecord() {
        when(vehicleMapper.selectOne(any())).thenReturn(vehicle(100L, 10L));
        when(parkingRecordMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> parkingRecordService.manualExit(new ManualExitRequest("粤B12345")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldGetMineById() {
        ParkingRecord record = parkingRecord(1L, 100L, 200L, "PARKING");
        when(parkingRecordMapper.selectById(1L)).thenReturn(record);
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 10L));
        when(parkingRecordConvert.toVO(record)).thenReturn(recordVO(1L, "PARKING"));

        ParkingRecordVO result = parkingRecordService.getMineById(10L, 1L);

        assertThat(result.recordId()).isEqualTo(1L);
    }

    @Test
    void shouldRejectGettingAnotherUsersParkingRecord() {
        when(parkingRecordMapper.selectById(1L)).thenReturn(parkingRecord(1L, 100L, 200L, "PARKING"));
        when(vehicleMapper.selectById(100L)).thenReturn(vehicle(100L, 20L));

        assertThatThrownBy(() -> parkingRecordService.getMineById(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPageParkingRecordsForAdmin() {
        ParkingRecord record = parkingRecord(1L, 100L, 200L, "PARKING");
        Page<ParkingRecord> page = Page.of(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(parkingRecordMapper.selectPage(any(), any())).thenReturn(page);
        when(parkingRecordConvert.toVOList(List.of(record))).thenReturn(List.of(recordVO(1L, "PARKING")));

        PageResponse<ParkingRecordVO> result = parkingRecordService.pageForAdmin(new ParkingRecordPageQuery(1L, 10L, "PARKING", null, null));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
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

    private Reservation reservation(Long reservationId, Long userId, Long vehicleId, Long spaceId, String status, LocalDateTime arriveTime) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(reservationId);
        reservation.setUserId(userId);
        reservation.setVehicleId(vehicleId);
        reservation.setSpaceId(spaceId);
        reservation.setStatus(status);
        reservation.setArriveTime(arriveTime);
        return reservation;
    }

    private ParkingRecord parkingRecord(Long recordId, Long vehicleId, Long spaceId, String status) {
        ParkingRecord record = new ParkingRecord();
        record.setRecordId(recordId);
        record.setVehicleId(vehicleId);
        record.setSpaceId(spaceId);
        record.setEntryTime(LocalDateTime.now().minusMinutes(30));
        record.setStatus(status);
        return record;
    }

    private ParkingRecordVO recordVO(Long recordId, String status) {
        return new ParkingRecordVO(recordId, 100L, 200L, LocalDateTime.now().minusMinutes(30), null, null, null, status, null, null);
    }
}
