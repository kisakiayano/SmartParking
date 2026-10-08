package com.smartparking.parking.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.parking.dto.CreateParkingSpaceRequest;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.parking.dto.UpdateParkingSpaceRequest;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;

public interface ParkingSpaceService {

    ParkingSpaceVO create(CreateParkingSpaceRequest request);

    ParkingSpaceVO update(Long spaceId, UpdateParkingSpaceRequest request);

    void delete(Long spaceId);

    ParkingSpaceVO getById(Long spaceId);

    PageResponse<ParkingSpaceVO> page(ParkingSpaceQuery query);

    ParkingSpaceStatusVO getRealtimeStatus(Long spaceId);
}
