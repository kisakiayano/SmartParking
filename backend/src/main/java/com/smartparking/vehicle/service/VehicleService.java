package com.smartparking.vehicle.service;

import com.smartparking.common.response.PageResponse;
import com.smartparking.vehicle.dto.CreateVehicleRequest;
import com.smartparking.vehicle.dto.UpdateVehicleRequest;
import com.smartparking.vehicle.dto.VehiclePageQuery;
import com.smartparking.vehicle.vo.VehicleVO;

import java.util.List;

public interface VehicleService {

    VehicleVO create(Long currentUserId, CreateVehicleRequest request);

    VehicleVO update(Long currentUserId, Long vehicleId, UpdateVehicleRequest request);

    void delete(Long currentUserId, Long vehicleId);

    VehicleVO getById(Long currentUserId, Long vehicleId);

    List<VehicleVO> listByCurrentUser(Long currentUserId);

    List<VehicleVO> listByUserId(Long userId);

    PageResponse<VehicleVO> pageForAdmin(VehiclePageQuery query);
}
