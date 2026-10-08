package com.smartparking.parking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import com.smartparking.parking.convert.ParkingSpaceConvert;
import com.smartparking.parking.dto.CreateParkingSpaceRequest;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.parking.dto.UpdateParkingSpaceRequest;
import com.smartparking.parking.entity.ParkingSpace;
import com.smartparking.parking.enums.ParkingSpaceStatus;
import com.smartparking.parking.mapper.ParkingSpaceMapper;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;
import com.smartparking.record.entity.ParkingRecord;
import com.smartparking.record.mapper.ParkingRecordMapper;
import com.smartparking.reservation.entity.Reservation;
import com.smartparking.reservation.mapper.ReservationMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ParkingSpaceServiceImpl implements ParkingSpaceService {

    private static final String CACHE_KEY_PREFIX = "parking:space:";

    private final ParkingSpaceMapper parkingSpaceMapper;
    private final ParkingSpaceConvert parkingSpaceConvert;
    private final ReservationMapper reservationMapper;
    private final ParkingRecordMapper parkingRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public ParkingSpaceServiceImpl(
            ParkingSpaceMapper parkingSpaceMapper,
            ParkingSpaceConvert parkingSpaceConvert,
            ReservationMapper reservationMapper,
            ParkingRecordMapper parkingRecordMapper,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.parkingSpaceMapper = parkingSpaceMapper;
        this.parkingSpaceConvert = parkingSpaceConvert;
        this.reservationMapper = reservationMapper;
        this.parkingRecordMapper = parkingRecordMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ParkingSpaceVO create(CreateParkingSpaceRequest request) {
        LocalDateTime now = LocalDateTime.now();
        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setArea(request.area());
        parkingSpace.setType(request.type());
        parkingSpace.setStatus(normalizeStatus(request.status()));
        parkingSpace.setVersion(0);
        parkingSpace.setCreateTime(now);
        parkingSpace.setUpdateTime(now);
        parkingSpaceMapper.insert(parkingSpace);
        cacheStatus(parkingSpace);
        return parkingSpaceConvert.toVO(parkingSpace);
    }

    @Override
    @Transactional
    public ParkingSpaceVO update(Long spaceId, UpdateParkingSpaceRequest request) {
        ParkingSpace parkingSpace = requireParkingSpace(spaceId);
        parkingSpace.setArea(request.area());
        parkingSpace.setType(request.type());
        parkingSpace.setStatus(normalizeStatus(request.status()));
        parkingSpace.setVersion(request.version());
        parkingSpace.setUpdateTime(LocalDateTime.now());

        int updated = parkingSpaceMapper.updateById(parkingSpace);
        if (updated == 0) {
            evictStatus(spaceId);
            throw new BusinessException(ResultCode.CONFLICT, "parking space version conflict");
        }
        cacheStatus(parkingSpace);
        return parkingSpaceConvert.toVO(parkingSpace);
    }

    @Override
    @Transactional
    public void delete(Long spaceId) {
        ParkingSpace parkingSpace = requireParkingSpace(spaceId);
        if (!ParkingSpaceStatus.FREE.name().equals(parkingSpace.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "only FREE parking spaces can be deleted");
        }
        ensureNoReferences(spaceId);
        // 条件删除（status + version）兜底与入场占用的并发，0 行说明车位状态已被他方修改
        int deleted = parkingSpaceMapper.delete(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getSpaceId, spaceId)
                .eq(ParkingSpace::getStatus, ParkingSpaceStatus.FREE.name())
                .eq(ParkingSpace::getVersion, parkingSpace.getVersion()));
        if (deleted == 0) {
            evictStatus(spaceId);
            throw new BusinessException(ResultCode.CONFLICT, "parking space status changed");
        }
        evictStatus(spaceId);
    }

    /**
     * 预约与停车记录通过外键引用车位且 space_id NOT NULL，
     * 存在任何引用时物理删除必然被 FK 拒绝，提前拦截返回业务冲突。
     */
    private void ensureNoReferences(Long spaceId) {
        if (reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getSpaceId, spaceId)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "parking space is referenced by reservations and cannot be deleted");
        }
        if (parkingRecordMapper.selectCount(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getSpaceId, spaceId)) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "parking space is referenced by parking records and cannot be deleted");
        }
    }

    @Override
    public ParkingSpaceVO getById(Long spaceId) {
        return parkingSpaceConvert.toVO(requireParkingSpace(spaceId));
    }

    @Override
    public PageResponse<ParkingSpaceVO> page(ParkingSpaceQuery query) {
        ParkingSpaceQuery normalizedQuery = query == null
                ? new ParkingSpaceQuery(null, null, null, null, null)
                : query;
        LambdaQueryWrapper<ParkingSpace> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(normalizedQuery.area())) {
            wrapper.eq(ParkingSpace::getArea, normalizedQuery.area());
        }
        if (StringUtils.hasText(normalizedQuery.type())) {
            wrapper.eq(ParkingSpace::getType, normalizedQuery.type());
        }
        if (StringUtils.hasText(normalizedQuery.status())) {
            wrapper.eq(ParkingSpace::getStatus, normalizeStatus(normalizedQuery.status()));
        }
        wrapper.orderByAsc(ParkingSpace::getArea).orderByAsc(ParkingSpace::getSpaceId);

        IPage<ParkingSpace> page = parkingSpaceMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                parkingSpaceConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    public ParkingSpaceStatusVO getRealtimeStatus(Long spaceId) {
        String key = cacheKey(spaceId);
        // 缓存读失败（含旧格式数据无法反序列化）时降级回数据库，不让缓存问题阻断业务
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof ParkingSpaceStatusVO statusVO) {
                return statusVO;
            }
            if (cached instanceof Map<?, ?> cachedMap) {
                ParkingSpaceStatusVO statusVO = fromCachedMap(cachedMap);
                if (statusVO != null) {
                    return statusVO;
                }
            }
        } catch (org.springframework.data.redis.serializer.SerializationException ex) {
            evictStatus(spaceId);
        } catch (RuntimeException ignored) {
            // Redis 不可用时不影响业务读取
        }
        ParkingSpace parkingSpace = requireParkingSpace(spaceId);
        ParkingSpaceStatusVO statusVO = parkingSpaceConvert.toStatusVO(parkingSpace);
        try {
            redisTemplate.opsForValue().set(key, statusVO);
        } catch (RuntimeException ignored) {
            // 缓存写失败由下次读取回填
        }
        return statusVO;
    }

    private ParkingSpace requireParkingSpace(Long spaceId) {
        ParkingSpace parkingSpace = parkingSpaceMapper.selectById(spaceId);
        if (parkingSpace == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "parking space not found");
        }
        return parkingSpace;
    }

    private String normalizeStatus(String status) {
        try {
            return ParkingSpaceStatus.valueOf(status).name();
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "invalid parking space status");
        }
    }

    private void cacheStatus(ParkingSpace parkingSpace) {
        try {
            redisTemplate.opsForValue().set(cacheKey(parkingSpace.getSpaceId()), parkingSpaceConvert.toStatusVO(parkingSpace));
        } catch (RuntimeException ex) {
            evictStatus(parkingSpace.getSpaceId());
        }
    }

    private void evictStatus(Long spaceId) {
        try {
            redisTemplate.delete(cacheKey(spaceId));
        } catch (RuntimeException ignored) {
            // Redis failures should not roll back the database transaction in this phase.
        }
    }

    private String cacheKey(Long spaceId) {
        return CACHE_KEY_PREFIX + spaceId;
    }

    private ParkingSpaceStatusVO fromCachedMap(Map<?, ?> cachedMap) {
        Object spaceId = cachedMap.get("spaceId");
        Object area = cachedMap.get("area");
        Object type = cachedMap.get("type");
        Object status = cachedMap.get("status");
        Object version = cachedMap.get("version");
        if (spaceId == null || status == null) {
            return null;
        }
        return new ParkingSpaceStatusVO(
                Long.valueOf(spaceId.toString()),
                area == null ? null : area.toString(),
                type == null ? null : type.toString(),
                status.toString(),
                version == null ? null : Integer.valueOf(version.toString())
        );
    }
}
