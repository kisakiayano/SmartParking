package com.smartparking.parking.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.parking.convert.ParkingSpaceConvert;
import com.smartparking.parking.dto.CreateParkingSpaceRequest;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.parking.dto.UpdateParkingSpaceRequest;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.parking.service.impl.ParkingSpaceServiceImpl;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.reservation.mapper.ReservationMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceImplTest {

    @Mock
    private ParkingSpaceMapper parkingSpaceMapper;

    @Mock
    private ParkingSpaceConvert parkingSpaceConvert;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ParkingRecordMapper parkingRecordMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ParkingSpaceServiceImpl parkingSpaceService;

    @Test
    void shouldCreateParkingSpaceAndCacheStatus() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingSpaceConvert.toStatusVO(any())).thenReturn(statusVO(1L, "FREE"));
        when(parkingSpaceConvert.toVO(any())).thenReturn(spaceVO(1L, "FREE", 0));

        ParkingSpaceVO result = parkingSpaceService.create(new CreateParkingSpaceRequest("A", "STANDARD", "FREE"));

        ArgumentCaptor<ParkingSpace> captor = ArgumentCaptor.forClass(ParkingSpace.class);
        verify(parkingSpaceMapper).insert(captor.capture());
        ParkingSpace inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(ParkingSpaceStatus.FREE.name());
        assertThat(inserted.getVersion()).isZero();
        verify(valueOperations).set(org.mockito.ArgumentMatchers.startsWith("parking:space:"), any());
        assertThat(result.status()).isEqualTo("FREE");
    }

    @Test
    void shouldUpdateParkingSpaceWithVersion() {
        ParkingSpace parkingSpace = parkingSpace(1L, "FREE", 0);
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace);
        when(parkingSpaceMapper.updateById(parkingSpace)).thenReturn(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(parkingSpaceConvert.toStatusVO(parkingSpace)).thenReturn(statusVO(1L, "RESERVED"));
        when(parkingSpaceConvert.toVO(parkingSpace)).thenReturn(spaceVO(1L, "RESERVED", 0));

        ParkingSpaceVO result = parkingSpaceService.update(1L, new UpdateParkingSpaceRequest("A", "STANDARD", "RESERVED", 0));

        assertThat(parkingSpace.getStatus()).isEqualTo("RESERVED");
        assertThat(parkingSpace.getVersion()).isZero();
        verify(parkingSpaceMapper).updateById(parkingSpace);
        verify(valueOperations).set("parking:space:1", statusVO(1L, "RESERVED"));
        assertThat(result.status()).isEqualTo("RESERVED");
    }

    @Test
    void shouldRejectVersionConflict() {
        ParkingSpace parkingSpace = parkingSpace(1L, "FREE", 0);
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace);
        when(parkingSpaceMapper.updateById(parkingSpace)).thenReturn(0);

        assertThatThrownBy(() -> parkingSpaceService.update(1L, new UpdateParkingSpaceRequest("A", "STANDARD", "FREE", 0)))
                .isInstanceOf(BusinessException.class);
        verify(redisTemplate).delete("parking:space:1");
    }

    @Test
    void shouldRejectDeletingNonFreeParkingSpace() {
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace(1L, "OCCUPIED", 2));

        assertThatThrownBy(() -> parkingSpaceService.delete(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldDeleteFreeParkingSpaceConditionallyAndEvictCache() {
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace(1L, "FREE", 0));
        when(parkingSpaceMapper.delete(any())).thenReturn(1);

        parkingSpaceService.delete(1L);

        verify(parkingSpaceMapper).delete(any());
        verify(redisTemplate).delete("parking:space:1");
    }

    @Test
    void shouldRejectDeletingParkingSpaceWhenVersionConflict() {
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace(1L, "FREE", 0));
        when(parkingSpaceMapper.delete(any())).thenReturn(0);

        assertThatThrownBy(() -> parkingSpaceService.delete(1L)).isInstanceOf(BusinessException.class);
        verify(redisTemplate).delete("parking:space:1");
    }

    @Test
    void shouldRejectDeletingParkingSpaceReferencedByReservations() {
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace(1L, "FREE", 0));
        when(reservationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> parkingSpaceService.delete(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectDeletingParkingSpaceReferencedByParkingRecords() {
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace(1L, "FREE", 0));
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingRecordMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> parkingSpaceService.delete(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldPageParkingSpaces() {
        ParkingSpace parkingSpace = parkingSpace(1L, "FREE", 0);
        Page<ParkingSpace> page = Page.of(1, 10);
        page.setRecords(List.of(parkingSpace));
        page.setTotal(1);
        when(parkingSpaceMapper.selectPage(any(), any())).thenReturn(page);
        when(parkingSpaceConvert.toVOList(List.of(parkingSpace))).thenReturn(List.of(spaceVO(1L, "FREE", 0)));

        PageResponse<ParkingSpaceVO> result = parkingSpaceService.page(new ParkingSpaceQuery(1L, 10L, "A", "STANDARD", "FREE"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
    }

    @Test
    void shouldReturnRealtimeStatusFromCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:space:1")).thenReturn(statusVO(1L, "FREE"));

        ParkingSpaceStatusVO result = parkingSpaceService.getRealtimeStatus(1L);

        assertThat(result.spaceId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("FREE");
    }

    @Test
    void shouldReturnRealtimeStatusFromCachedMap() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:space:1")).thenReturn(Map.of(
                "spaceId", 1,
                "area", "A",
                "type", "STANDARD",
                "status", "FREE",
                "version", 0
        ));

        ParkingSpaceStatusVO result = parkingSpaceService.getRealtimeStatus(1L);

        assertThat(result.spaceId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("FREE");
    }

    @Test
    void shouldLoadRealtimeStatusFromDbAndCacheWhenMissing() {
        ParkingSpace parkingSpace = parkingSpace(1L, "FREE", 0);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("parking:space:1")).thenReturn(null);
        when(parkingSpaceMapper.selectById(1L)).thenReturn(parkingSpace);
        when(parkingSpaceConvert.toStatusVO(parkingSpace)).thenReturn(statusVO(1L, "FREE"));

        ParkingSpaceStatusVO result = parkingSpaceService.getRealtimeStatus(1L);

        verify(valueOperations).set("parking:space:1", result);
        assertThat(result.status()).isEqualTo("FREE");
    }

    private ParkingSpace parkingSpace(Long spaceId, String status, Integer version) {
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setSpaceId(spaceId);
        parkingSpace.setArea("A");
        parkingSpace.setType("STANDARD");
        parkingSpace.setStatus(status);
        parkingSpace.setVersion(version);
        parkingSpace.setCreateTime(LocalDateTime.now());
        parkingSpace.setUpdateTime(LocalDateTime.now());
        return parkingSpace;
    }

    private ParkingSpaceVO spaceVO(Long spaceId, String status, Integer version) {
        return new ParkingSpaceVO(spaceId, "A", "STANDARD", status, version, null, null);
    }

    private ParkingSpaceStatusVO statusVO(Long spaceId, String status) {
        return new ParkingSpaceStatusVO(spaceId, "A", "STANDARD", status, 0);
    }
}
