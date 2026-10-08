package com.smartparking.vehicle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.vehicle.convert.VehicleConvert;
import com.smartparking.vehicle.dto.CreateVehicleRequest;
import com.smartparking.vehicle.dto.UpdateVehicleRequest;
import com.smartparking.vehicle.dto.VehiclePageQuery;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final String DEFAULT_ORDER = "desc";

    private final VehicleMapper vehicleMapper;
    private final VehicleConvert vehicleConvert;
    private final ReservationMapper reservationMapper;
    private final ParkingRecordMapper parkingRecordMapper;

    public VehicleServiceImpl(
            VehicleMapper vehicleMapper,
            VehicleConvert vehicleConvert,
            ReservationMapper reservationMapper,
            ParkingRecordMapper parkingRecordMapper
    ) {
        this.vehicleMapper = vehicleMapper;
        this.vehicleConvert = vehicleConvert;
        this.reservationMapper = reservationMapper;
        this.parkingRecordMapper = parkingRecordMapper;
    }

    @Override
    @Transactional
    public VehicleVO create(Long currentUserId, CreateVehicleRequest request) {
        String plateNumber = normalizePlateNumber(request.plateNumber());
        if (existsByPlateNumber(plateNumber, null)) {
            throw new BusinessException(ResultCode.CONFLICT, "plate number already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setUserId(currentUserId);
        vehicle.setCreateTime(now);
        vehicle.setUpdateTime(now);
        vehicleMapper.insert(vehicle);
        return vehicleConvert.toVO(vehicle);
    }

    @Override
    @Transactional
    public VehicleVO update(Long currentUserId, Long vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = requireOwnedVehicle(currentUserId, vehicleId);
        String plateNumber = normalizePlateNumber(request.plateNumber());
        if (existsByPlateNumber(plateNumber, vehicleId)) {
            throw new BusinessException(ResultCode.CONFLICT, "plate number already exists");
        }

        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setUpdateTime(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
        return vehicleConvert.toVO(vehicle);
    }

    @Override
    @Transactional
    public void delete(Long currentUserId, Long vehicleId) {
        Vehicle vehicle = requireOwnedVehicle(currentUserId, vehicleId);
        ensureNoReferences(vehicle.getVehicleId());
        vehicleMapper.deleteById(vehicle.getVehicleId());
    }

    @Override
    public VehicleVO getById(Long currentUserId, Long vehicleId) {
        return vehicleConvert.toVO(requireOwnedVehicle(currentUserId, vehicleId));
    }

    @Override
    public List<VehicleVO> listByCurrentUser(Long currentUserId) {
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getUserId, currentUserId)
                .orderByDesc(Vehicle::getCreateTime));
        return vehicleConvert.toVOList(vehicles);
    }

    @Override
    public List<VehicleVO> listByUserId(Long userId) {
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getUserId, userId)
                .orderByDesc(Vehicle::getCreateTime));
        return vehicleConvert.toVOList(vehicles);
    }

    @Override
    public PageResponse<VehicleVO> pageForAdmin(VehiclePageQuery query) {
        VehiclePageQuery normalizedQuery = query == null
                ? new VehiclePageQuery(null, null, null, null, null, null, null)
                : query;
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(normalizedQuery.plateNumber())) {
            wrapper.like(Vehicle::getPlateNumber, normalizePlateNumber(normalizedQuery.plateNumber()));
        }
        if (StringUtils.hasText(normalizedQuery.vehicleType())) {
            wrapper.eq(Vehicle::getVehicleType, normalizedQuery.vehicleType());
        }
        if (normalizedQuery.userId() != null) {
            wrapper.eq(Vehicle::getUserId, normalizedQuery.userId());
        }
        applyOrder(wrapper, normalizedQuery);

        IPage<Vehicle> page = vehicleMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                vehicleConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private Vehicle requireOwnedVehicle(Long currentUserId, Long vehicleId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "vehicle not found");
        }
        if (!vehicle.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot manage another user's vehicle");
        }
        return vehicle;
    }

    /**
     * 预约与停车记录通过外键引用车辆且 vehicle_id NOT NULL，
     * 存在任何引用时物理删除必然被 FK 拒绝，提前拦截返回业务冲突。
     */
    private void ensureNoReferences(Long vehicleId) {
        if (reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getVehicleId, vehicleId)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "vehicle is referenced by reservations and cannot be deleted");
        }
        if (parkingRecordMapper.selectCount(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getVehicleId, vehicleId)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "vehicle is referenced by parking records and cannot be deleted");
        }
    }

    private boolean existsByPlateNumber(String plateNumber, Long excludeVehicleId) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNumber, plateNumber);
        if (excludeVehicleId != null) {
            wrapper.ne(Vehicle::getVehicleId, excludeVehicleId);
        }
        return vehicleMapper.selectCount(wrapper) > 0;
    }

    private String normalizePlateNumber(String plateNumber) {
        return plateNumber == null ? null : plateNumber.trim().toUpperCase();
    }

    private void applyOrder(LambdaQueryWrapper<Vehicle> wrapper, VehiclePageQuery query) {
        String sortBy = StringUtils.hasText(query.sortBy()) ? query.sortBy() : "createTime";
        String order = StringUtils.hasText(query.order()) ? query.order() : DEFAULT_ORDER;
        boolean asc = "asc".equalsIgnoreCase(order);
        switch (sortBy) {
            case "plateNumber" -> wrapper.orderBy(true, asc, Vehicle::getPlateNumber);
            case "updateTime" -> wrapper.orderBy(true, asc, Vehicle::getUpdateTime);
            default -> wrapper.orderBy(true, asc, Vehicle::getCreateTime);
        }
    }
}
