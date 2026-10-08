package com.smartparking.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.reservation.convert.ReservationConvert;
import com.smartparking.reservation.dto.CreateReservationRequest;
import com.smartparking.reservation.dto.ReservationPageQuery;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.enums.ReservationStatus;
import com.smartparking.reservation.mapper.ReservationMapper;
import com.smartparking.reservation.service.ReservationService;
import com.smartparking.reservation.vo.ReservationVO;
import com.smartparking.vehicle.entity.Vehicle;
import com.smartparking.vehicle.mapper.VehicleMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final String PARKING_SPACE_CACHE_PREFIX = "parking:space:";

    private final ReservationMapper reservationMapper;
    private final ReservationConvert reservationConvert;
    private final VehicleMapper vehicleMapper;
    private final ParkingSpaceMapper parkingSpaceMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public ReservationServiceImpl(
            ReservationMapper reservationMapper,
            ReservationConvert reservationConvert,
            VehicleMapper vehicleMapper,
            ParkingSpaceMapper parkingSpaceMapper,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.reservationMapper = reservationMapper;
        this.reservationConvert = reservationConvert;
        this.vehicleMapper = vehicleMapper;
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ReservationVO create(Long currentUserId, CreateReservationRequest request) {
        Vehicle vehicle = requireOwnedVehicle(currentUserId, request.vehicleId());
        if (existsActiveReservationForUser(currentUserId)) {
            throw new BusinessException(ResultCode.CONFLICT, "user already has an ACTIVE reservation");
        }

        ParkingSpace parkingSpace = requireParkingSpace(request.spaceId());
        if (!ParkingSpaceStatus.FREE.name().equals(parkingSpace.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "parking space is not available");
        }
        lockParkingSpace(parkingSpace, ParkingSpaceStatus.RESERVED);

        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = new Reservation();
        reservation.setUserId(currentUserId);
        reservation.setVehicleId(vehicle.getVehicleId());
        reservation.setSpaceId(parkingSpace.getSpaceId());
        reservation.setReserveTime(now);
        reservation.setArriveTime(request.arriveTime());
        reservation.setStatus(ReservationStatus.ACTIVE.name());
        reservation.setCreateTime(now);
        reservation.setUpdateTime(now);
        reservationMapper.insert(reservation);
        cacheParkingSpaceStatus(parkingSpace);
        return reservationConvert.toVO(reservation);
    }

    @Override
    public ReservationVO getById(Long currentUserId, Long reservationId) {
        Reservation reservation = requireOwnedReservation(currentUserId, reservationId);
        return reservationConvert.toVO(reservation);
    }

    @Override
    @Transactional
    public void cancel(Long currentUserId, Long reservationId) {
        Reservation reservation = requireOwnedReservation(currentUserId, reservationId);
        if (!ReservationStatus.ACTIVE.name().equals(reservation.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "only ACTIVE reservations can be cancelled");
        }

        ParkingSpace parkingSpace = requireParkingSpace(reservation.getSpaceId());
        releaseParkingSpace(parkingSpace);

        reservation.setStatus(ReservationStatus.CANCELLED.name());
        reservation.setUpdateTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        cacheParkingSpaceStatus(parkingSpace);
    }

    @Override
    public PageResponse<ReservationVO> pageMine(Long currentUserId, ReservationPageQuery query) {
        ReservationPageQuery normalizedQuery = query == null
                ? new ReservationPageQuery(null, null, null, null, null, null)
                : query;
        LambdaQueryWrapper<Reservation> wrapper = buildQueryWrapper(normalizedQuery)
                .eq(Reservation::getUserId, currentUserId);
        return toPageResponse(reservationMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        ));
    }

    @Override
    public PageResponse<ReservationVO> pageForAdmin(ReservationPageQuery query) {
        ReservationPageQuery normalizedQuery = query == null
                ? new ReservationPageQuery(null, null, null, null, null, null)
                : query;
        return toPageResponse(reservationMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                buildQueryWrapper(normalizedQuery)
        ));
    }

    private Vehicle requireOwnedVehicle(Long currentUserId, Long vehicleId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "vehicle not found");
        }
        if (!vehicle.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot reserve with another user's vehicle");
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

    private Reservation requireOwnedReservation(Long currentUserId, Long reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "reservation not found");
        }
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot manage another user's reservation");
        }
        return reservation;
    }

    private boolean existsActiveReservationForUser(Long userId) {
        return reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getStatus, ReservationStatus.ACTIVE.name())) > 0;
    }

    private void lockParkingSpace(ParkingSpace parkingSpace, ParkingSpaceStatus targetStatus) {
        int updated = parkingSpaceMapper.update(null, new UpdateWrapper<ParkingSpace>()
                .eq("space_id", parkingSpace.getSpaceId())
                .eq("status", ParkingSpaceStatus.FREE.name())
                .eq("version", parkingSpace.getVersion())
                .set("status", targetStatus.name())
                .set("update_time", LocalDateTime.now())
                .setSql("version = version + 1"));
        if (updated == 0) {
            evictParkingSpaceStatus(parkingSpace.getSpaceId());
            throw new BusinessException(ResultCode.CONFLICT, "parking space is not available");
        }
        parkingSpace.setStatus(targetStatus.name());
        parkingSpace.setVersion(parkingSpace.getVersion() + 1);
        parkingSpace.setUpdateTime(LocalDateTime.now());
    }

    private void releaseParkingSpace(ParkingSpace parkingSpace) {
        int updated = parkingSpaceMapper.update(null, new UpdateWrapper<ParkingSpace>()
                .eq("space_id", parkingSpace.getSpaceId())
                .eq("status", ParkingSpaceStatus.RESERVED.name())
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

    private LambdaQueryWrapper<Reservation> buildQueryWrapper(ReservationPageQuery query) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(Reservation::getStatus, normalizeStatus(query.status()));
        }
        if (query.userId() != null) {
            wrapper.eq(Reservation::getUserId, query.userId());
        }
        if (query.vehicleId() != null) {
            wrapper.eq(Reservation::getVehicleId, query.vehicleId());
        }
        if (query.spaceId() != null) {
            wrapper.eq(Reservation::getSpaceId, query.spaceId());
        }
        wrapper.orderByDesc(Reservation::getCreateTime);
        return wrapper;
    }

    private String normalizeStatus(String status) {
        try {
            return ReservationStatus.valueOf(status).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "invalid reservation status");
        }
    }

    private PageResponse<ReservationVO> toPageResponse(IPage<Reservation> page) {
        return new PageResponse<>(
                reservationConvert.toVOList(page.getRecords()),
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
