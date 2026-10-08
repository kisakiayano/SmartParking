package com.smartparking.parking.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.parking.dto.ParkingSpaceQuery;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceStatusVO;
import com.smartparking.parking.vo.ParkingSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "车位接口")
@Validated
@RestController
@RequestMapping("/api/v1/parking-spaces")
@SecurityRequirement(name = "bearerAuth")
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    public ParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    @Operation(summary = "分页查询车位")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<ParkingSpaceVO>> page(@Valid ParkingSpaceQuery query) {
        return ApiResponse.success(parkingSpaceService.page(query));
    }

    @Operation(summary = "查询车位详情")
    @GetMapping("/{spaceId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ParkingSpaceVO> getById(@PathVariable @NotNull Long spaceId) {
        return ApiResponse.success(parkingSpaceService.getById(spaceId));
    }

    @Operation(summary = "查询车位实时状态")
    @GetMapping("/{spaceId}/status")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ParkingSpaceStatusVO> getRealtimeStatus(@PathVariable @NotNull Long spaceId) {
        return ApiResponse.success(parkingSpaceService.getRealtimeStatus(spaceId));
    }
}
