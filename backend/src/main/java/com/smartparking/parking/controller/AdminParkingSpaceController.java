package com.smartparking.parking.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.parking.dto.CreateParkingSpaceRequest;
import com.smartparking.parking.dto.UpdateParkingSpaceRequest;
import com.smartparking.parking.service.ParkingSpaceService;
import com.smartparking.parking.vo.ParkingSpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员车位接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/parking-spaces")
@SecurityRequirement(name = "bearerAuth")
public class AdminParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    public AdminParkingSpaceController(ParkingSpaceService parkingSpaceService) {
        this.parkingSpaceService = parkingSpaceService;
    }

    @Operation(summary = "新增车位")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParkingSpaceVO> create(@Valid @RequestBody CreateParkingSpaceRequest request) {
        return ApiResponse.success(parkingSpaceService.create(request));
    }

    @Operation(summary = "修改车位")
    @PutMapping("/{spaceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParkingSpaceVO> update(
            @PathVariable @NotNull Long spaceId,
            @Valid @RequestBody UpdateParkingSpaceRequest request
    ) {
        return ApiResponse.success(parkingSpaceService.update(spaceId, request));
    }

    @Operation(summary = "删除车位")
    @DeleteMapping("/{spaceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable @NotNull Long spaceId) {
        parkingSpaceService.delete(spaceId);
        return ApiResponse.success();
    }
}
