package com.smartparking.vehicle.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.vehicle.convert.VehicleConvert;
import com.smartparking.vehicle.dto.CreateVehicleRequest;
import com.smartparking.vehicle.dto.UpdateVehicleRequest;
import com.smartparking.vehicle.dto.VehiclePageQuery;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import com.smartparking.vehicle.service.impl.VehicleServiceImpl;
import com.smartparking.vehicle.vo.VehicleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private VehicleConvert vehicleConvert;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ParkingRecordMapper parkingRecordMapper;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    void shouldCreateVehicleForCurrentUser() {
        when(vehicleMapper.selectCount(any())).thenReturn(0L);
        when(vehicleConvert.toVO(any())).thenReturn(vehicleVO(1L, "粤B12345", 10L));

        VehicleVO result = vehicleService.create(10L, new CreateVehicleRequest("粤B12345", "SMALL"));

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleMapper).insert(captor.capture());
        Vehicle inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10L);
        assertThat(inserted.getPlateNumber()).isEqualTo("粤B12345");
        assertThat(result.userId()).isEqualTo(10L);
    }

    @Test
    void shouldRejectDuplicatePlateNumberOnCreate() {
        when(vehicleMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> vehicleService.create(10L, new CreateVehicleRequest("粤B12345", "SMALL")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldUpdateOwnedVehicle() {
        Vehicle vehicle = vehicle(1L, "粤B12345", 10L);
        when(vehicleMapper.selectById(1L)).thenReturn(vehicle);
        when(vehicleMapper.selectCount(any())).thenReturn(0L);
        when(vehicleConvert.toVO(vehicle)).thenReturn(vehicleVO(1L, "粤B99999", 10L));

        VehicleVO result = vehicleService.update(10L, 1L, new UpdateVehicleRequest("粤B99999", "SUV"));

        verify(vehicleMapper).updateById(vehicle);
        assertThat(vehicle.getPlateNumber()).isEqualTo("粤B99999");
        assertThat(vehicle.getVehicleType()).isEqualTo("SUV");
        assertThat(result.plateNumber()).isEqualTo("粤B99999");
    }

    @Test
    void shouldRejectUpdatingAnotherUsersVehicle() {
        when(vehicleMapper.selectById(1L)).thenReturn(vehicle(1L, "粤B12345", 20L));

        assertThatThrownBy(() -> vehicleService.update(10L, 1L, new UpdateVehicleRequest("粤B99999", "SUV")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldDeleteOwnedVehicleWithoutReferences() {
        Vehicle vehicle = vehicle(1L, "粤B12345", 10L);
        when(vehicleMapper.selectById(1L)).thenReturn(vehicle);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingRecordMapper.selectCount(any())).thenReturn(0L);

        vehicleService.delete(10L, 1L);

        verify(vehicleMapper).deleteById(1L);
    }

    @Test
    void shouldRejectDeletingVehicleWithReservations() {
        Vehicle vehicle = vehicle(1L, "粤B12345", 10L);
        when(vehicleMapper.selectById(1L)).thenReturn(vehicle);
        when(reservationMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> vehicleService.delete(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectDeletingVehicleWithParkingRecords() {
        Vehicle vehicle = vehicle(1L, "粤B12345", 10L);
        when(vehicleMapper.selectById(1L)).thenReturn(vehicle);
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(parkingRecordMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> vehicleService.delete(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldListCurrentUserVehicles() {
        List<Vehicle> vehicles = List.of(vehicle(1L, "粤B12345", 10L));
        when(vehicleMapper.selectList(any())).thenReturn(vehicles);
        when(vehicleConvert.toVOList(vehicles)).thenReturn(List.of(vehicleVO(1L, "粤B12345", 10L)));

        List<VehicleVO> result = vehicleService.listByCurrentUser(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(10L);
    }

    @Test
    void shouldPageVehiclesForAdmin() {
        Vehicle vehicle = vehicle(1L, "粤B12345", 10L);
        Page<Vehicle> page = Page.of(1, 10);
        page.setRecords(List.of(vehicle));
        page.setTotal(1);
        when(vehicleMapper.selectPage(any(), any())).thenReturn(page);
        when(vehicleConvert.toVOList(List.of(vehicle))).thenReturn(List.of(vehicleVO(1L, "粤B12345", 10L)));

        PageResponse<VehicleVO> result = vehicleService.pageForAdmin(new VehiclePageQuery(1L, 10L, "粤B", "SMALL", 10L, "createTime", "desc"));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        assertThat(result.pageNum()).isEqualTo(1);
    }

    private Vehicle vehicle(Long vehicleId, String plateNumber, Long userId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType("SMALL");
        vehicle.setUserId(userId);
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());
        return vehicle;
    }

    private VehicleVO vehicleVO(Long vehicleId, String plateNumber, Long userId) {
        return new VehicleVO(vehicleId, plateNumber, "SMALL", userId, null, null);
    }
}
