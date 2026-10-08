package com.smartparking.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.fee.service.FeeCalculatorService;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.record.convert.ParkingRecordConvert;
import com.smartparking.record.dto.ManualEntryRequest;
import com.smartparking.record.dto.ManualExitRequest;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.enums.ParkingRecordStatus;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import com.smartparking.reservation.config.ReservationProperties;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ParkingRecordServiceImpl implements ParkingRecordService {

    private static final String PARKING_SPACE_CACHE_PREFIX = "parking:space:";

    private final ParkingRecordMapper parkingRecordMapper;
    private final ParkingRecordConvert parkingRecordConvert;
    private final VehicleMapper vehicleMapper;
    private final ParkingSpaceMapper parkingSpaceMapper;
    private final ReservationMapper reservationMapper;
    private final FeeCalculatorService feeCalculatorService;
    private final ReservationProperties reservationProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    public ParkingRecordServiceImpl(
            ParkingRecordMapper parkingRecordMapper,
            ParkingRecordConvert parkingRecordConvert,
            VehicleMapper vehicleMapper,
            ParkingSpaceMapper parkingSpaceMapper,
            ReservationMapper reservationMapper,
            FeeCalculatorService feeCalculatorService,
            ReservationProperties reservationProperties,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.parkingRecordMapper = parkingRecordMapper;
        this.parkingRecordConvert = parkingRecordConvert;
        this.vehicleMapper = vehicleMapper;
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.reservationMapper = reservationMapper;
        this.feeCalculatorService = feeCalculatorService;
        this.reservationProperties = reservationProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ParkingRecordVO manualEntry(ManualEntryRequest request) {
        return entry(normalizePlateNumber(request.plateNumber()), request.spaceId());
    }

    @Override
    @Transactional
    public ParkingRecordVO manualExit(ManualExitRequest request) {
        return exit(normalizePlateNumber(request.plateNumber()));
    }

    @Override
    @Transactional
    public ParkingRecordVO autoEntry(String plateNumber, Long preferredSpaceId) {
        String normalizedPlateNumber = normalizePlateNumber(plateNumber);
        Long spaceId = preferredSpaceId == null ? resolveAutoEntrySpaceId(normalizedPlateNumber) : preferredSpaceId;
        return entry(normalizedPlateNumber, spaceId);
    }

    @Override
    @Transactional
    public ParkingRecordVO autoExit(String plateNumber) {
        return exit(normalizePlateNumber(plateNumber));
    }

    private ParkingRecordVO entry(String plateNumber, Long spaceId) {
        Vehicle vehicle = requireVehicleByPlateNumber(plateNumber);
        if (existsParkingRecord(vehicle.getVehicleId())) {
            throw new BusinessException(ResultCode.CONFLICT, "vehicle is already parking");
        }

        ParkingSpace parkingSpace = requireParkingSpace(spaceId);
        Reservation activeReservation = findActiveReservation(vehicle.getUserId(), vehicle.getVehicleId(), parkingSpace.getSpaceId());
        if (activeReservation != null && isReservationExpired(activeReservation)) {
            throw new BusinessException(ResultCode.CONFLICT, "reservation has expired");
        }
        if (activeReservation == null && !ParkingSpaceStatus.FREE.name().equals(parkingSpace.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "parking space is not available");
        }
        if (activeReservation != null && !ParkingSpaceStatus.RESERVED.name().equals(parkingSpace.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "reserved parking space status changed");
        }

        occupyParkingSpace(parkingSpace, activeReservation != null ? ParkingSpaceStatus.RESERVED : ParkingSpaceStatus.FREE);
        if (activeReservation != null) {
            markReservationUsed(activeReservation);
        }

        LocalDateTime now = LocalDateTime.now();
        ParkingRecord record = new ParkingRecord();
        record.setVehicleId(vehicle.getVehicleId());
        record.setSpaceId(parkingSpace.getSpaceId());
        record.setEntryTime(now);
        record.setStatus(ParkingRecordStatus.PARKING.name());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        parkingRecordMapper.insert(record);
        cacheParkingSpaceStatus(parkingSpace);
        return parkingRecordConvert.toVO(record);
    }

    private ParkingRecordVO exit(String plateNumber) {
        Vehicle vehicle = requireVehicleByPlateNumber(plateNumber);
        ParkingRecord record = parkingRecordMapper.selectOne(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getVehicleId, vehicle.getVehicleId())
                .eq(ParkingRecord::getStatus, ParkingRecordStatus.PARKING.name())
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "parking record not found");
        }

        ParkingSpace parkingSpace = requireParkingSpace(record.getSpaceId());
        LocalDateTime now = LocalDateTime.now();
        int durationMinutes = calculateDurationMinutes(record.getEntryTime(), now);
        BigDecimal fee = feeCalculatorService.calculate(record.getEntryTime(), now);
        releaseParkingSpace(parkingSpace);

        record.setExitTime(now);
        record.setDuration(durationMinutes);
        record.setFee(fee);
        record.setStatus(ParkingRecordStatus.FINISHED.name());
        record.setUpdateTime(now);
        parkingRecordMapper.updateById(record);
        cacheParkingSpaceStatus(parkingSpace);
        return parkingRecordConvert.toVO(record);
    }

    private Long resolveAutoEntrySpaceId(String plateNumber) {
        Vehicle vehicle = requireVehicleByPlateNumber(plateNumber);
        Reservation activeReservation = reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, vehicle.getUserId())
                .eq(Reservation::getVehicleId, vehicle.getVehicleId())
                .eq(Reservation::getStatus, ReservationStatus.ACTIVE.name())
                .gt(Reservation::getArriveTime, LocalDateTime.now().minusMinutes(reservationProperties.expirationGraceMinutes()))
                .last("LIMIT 1"));
        if (activeReservation != null) {
            return activeReservation.getSpaceId();
        }
        ParkingSpace parkingSpace = parkingSpaceMapper.selectOne(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getStatus, ParkingSpaceStatus.FREE.name())
                .orderByAsc(ParkingSpace::getSpaceId)
                .last("LIMIT 1"));
        if (parkingSpace == null) {
            throw new BusinessException(ResultCode.CONFLICT, "no free parking space available");
        }
        return parkingSpace.getSpaceId();
    }

    @Override
    public ParkingRecordVO getById(Long recordId) {
        ParkingRecord record = parkingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "parking record not found");
        }
        return parkingRecordConvert.toVO(record);
    }

    @Override
    public ParkingRecordVO getMineById(Long currentUserId, Long recordId) {
        ParkingRecord record = parkingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "parking record not found");
        }
        Vehicle vehicle = vehicleMapper.selectById(record.getVehicleId());
        if (vehicle == null || !vehicle.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot view another user's parking record");
        }
        return parkingRecordConvert.toVO(record);
    }

    @Override
    public PageResponse<ParkingRecordVO> pageMine(Long currentUserId, ParkingRecordPageQuery query) {
        ParkingRecordPageQuery normalizedQuery = query == null
                ? new ParkingRecordPageQuery(null, null, null, null, null)
                : query;
        LambdaQueryWrapper<Vehicle> vehicleWrapper = new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getUserId, currentUserId);
        if (normalizedQuery.vehicleId() != null) {
            vehicleWrapper.eq(Vehicle::getVehicleId, normalizedQuery.vehicleId());
        }
        var vehicles = vehicleMapper.selectList(vehicleWrapper);
        if (vehicles.isEmpty()) {
            return new PageResponse<>(java.util.List.of(), 0, normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize());
        }
        LambdaQueryWrapper<ParkingRecord> wrapper = buildQueryWrapper(normalizedQuery)
                .in(ParkingRecord::getVehicleId, vehicles.stream().map(Vehicle::getVehicleId).toList());
        return toPageResponse(parkingRecordMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        ));
    }

    @Override
    public PageResponse<ParkingRecordVO> pageForAdmin(ParkingRecordPageQuery query) {
        ParkingRecordPageQuery normalizedQuery = query == null
                ? new ParkingRecordPageQuery(null, null, null, null, null)
                : query;
        return toPageResponse(parkingRecordMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                buildQueryWrapper(normalizedQuery)
        ));
    }

    private Vehicle requireVehicleByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleMapper.selectOne(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNumber, plateNumber)
                .last("LIMIT 1"));
        if (vehicle == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "vehicle not found");
        }
        return vehicle;
    }

    private ParkingSpace requireParkingSpace(Long spaceId) {
        ParkingSpace parkingSpace = parkingSpaceMapper.selectById(spaceId);
        if (parkingSpace == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "parking space not found");
        }
        return parkingSpace;
    }

    private boolean existsParkingRecord(Long vehicleId) {
        return parkingRecordMapper.selectCount(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getVehicleId, vehicleId)
                .eq(ParkingRecord::getStatus, ParkingRecordStatus.PARKING.name())) > 0;
    }

    private Reservation findActiveReservation(Long userId, Long vehicleId, Long spaceId) {
        return reservationMapper.selectOne(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getVehicleId, vehicleId)
                .eq(Reservation::getSpaceId, spaceId)
                .eq(Reservation::getStatus, ReservationStatus.ACTIVE.name())
                .last("LIMIT 1"));
    }

    private boolean isReservationExpired(Reservation reservation) {
        LocalDateTime deadline = reservation.getArriveTime()
                .plusMinutes(reservationProperties.expirationGraceMinutes());
        return LocalDateTime.now().isAfter(deadline);
    }

    private void markReservationUsed(Reservation reservation) {
        int updated = reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("reservation_id", reservation.getReservationId())
                .eq("status", ReservationStatus.ACTIVE.name())
                .set("status", ReservationStatus.USED.name())
                .set("update_time", LocalDateTime.now()));
        if (updated == 0) {
            // 与过期任务/取消操作并发：预约状态已被他方推进，入场不应继续
            throw new BusinessException(ResultCode.CONFLICT, "reservation is no longer ACTIVE");
        }
    }

    private void occupyParkingSpace(ParkingSpace parkingSpace, ParkingSpaceStatus expectedStatus) {
        int updated = parkingSpaceMapper.update(null, new UpdateWrapper<ParkingSpace>()
                .eq("space_id", parkingSpace.getSpaceId())
                .eq("status", expectedStatus.name())
                .eq("version", parkingSpace.getVersion())
                .set("status", ParkingSpaceStatus.OCCUPIED.name())
                .set("update_time", LocalDateTime.now())
                .setSql("version = version + 1"));
        if (updated == 0) {
            evictParkingSpaceStatus(parkingSpace.getSpaceId());
            throw new BusinessException(ResultCode.CONFLICT, "parking space status changed");
        }
        parkingSpace.setStatus(ParkingSpaceStatus.OCCUPIED.name());
        parkingSpace.setVersion(parkingSpace.getVersion() + 1);
        parkingSpace.setUpdateTime(LocalDateTime.now());
    }

    private void releaseParkingSpace(ParkingSpace parkingSpace) {
        int updated = parkingSpaceMapper.update(null, new UpdateWrapper<ParkingSpace>()
                .eq("space_id", parkingSpace.getSpaceId())
                .eq("status", ParkingSpaceStatus.OCCUPIED.name())
                .eq("version", parkingSpace.getVersion())
                .set("status", ParkingSpaceStatus.FREE.name())
                .set("update_time", LocalDateTime.now())
                .setSql("version = version + 1"));
        if (updated == 0) {
            evictParkingSpaceStatus(parkingSpace.getSpaceId());
            throw new BusinessException(ResultCode.CONFLICT, "parking space status changed");
        }
        parkingSpace.setStatus(ParkingSpaceStatus.FREE.name());
        parkingSpace.setVersion(parkingSpace.getVersion() + 1);
        parkingSpace.setUpdateTime(LocalDateTime.now());
    }

    private LambdaQueryWrapper<ParkingRecord> buildQueryWrapper(ParkingRecordPageQuery query) {
        LambdaQueryWrapper<ParkingRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(ParkingRecord::getStatus, normalizeStatus(query.status()));
        }
        if (query.vehicleId() != null) {
            wrapper.eq(ParkingRecord::getVehicleId, query.vehicleId());
        }
        if (query.spaceId() != null) {
            wrapper.eq(ParkingRecord::getSpaceId, query.spaceId());
        }
        wrapper.orderByDesc(ParkingRecord::getEntryTime);
        return wrapper;
    }

    private String normalizeStatus(String status) {
        try {
            return ParkingRecordStatus.valueOf(status).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "invalid parking record status");
        }
    }

    private String normalizePlateNumber(String plateNumber) {
        return plateNumber == null ? null : plateNumber.trim().toUpperCase();
    }

    private int calculateDurationMinutes(LocalDateTime entryTime, LocalDateTime exitTime) {
        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        return Math.max(1, Math.toIntExact(minutes));
    }

    private PageResponse<ParkingRecordVO> toPageResponse(IPage<ParkingRecord> page) {
        return new PageResponse<>(
                parkingRecordConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private void cacheParkingSpaceStatus(ParkingSpace parkingSpace) {
        try {
            redisTemplate.opsForValue().set(
                    parkingSpaceCacheKey(parkingSpace.getSpaceId()),
                    new ParkingSpaceStatusVO(
                            parkingSpace.getSpaceId(),
                            parkingSpace.getArea(),
                            parkingSpace.getType(),
                            parkingSpace.getStatus(),
                            parkingSpace.getVersion()
                    )
            );
        } catch (RuntimeException ex) {
            evictParkingSpaceStatus(parkingSpace.getSpaceId());
        }
    }

    private void evictParkingSpaceStatus(Long spaceId) {
        try {
            redisTemplate.delete(parkingSpaceCacheKey(spaceId));
        } catch (RuntimeException ignored) {
            // Redis failure should not hide the database conflict or state change result.
        }
    }

    private String parkingSpaceCacheKey(Long spaceId) {
        return PARKING_SPACE_CACHE_PREFIX + spaceId;
    }
}
