package com.smartparking.vehicle.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.security.SecurityUtils;
import com.smartparking.vehicle.dto.CreateVehicleRequest;
import com.smartparking.vehicle.dto.UpdateVehicleRequest;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "车辆接口")
@Validated
@RestController
@RequestMapping("/api/v1/vehicles")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "新增车辆")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<VehicleVO> create(@Valid @RequestBody CreateVehicleRequest request) {
        return ApiResponse.success(vehicleService.create(SecurityUtils.currentUser().userId(), request));
    }

    @Operation(summary = "修改车辆")
    @PutMapping("/{vehicleId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<VehicleVO> update(
            @PathVariable @NotNull Long vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        return ApiResponse.success(vehicleService.update(SecurityUtils.currentUser().userId(), vehicleId, request));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> delete(@PathVariable @NotNull Long vehicleId) {
        vehicleService.delete(SecurityUtils.currentUser().userId(), vehicleId);
        return ApiResponse.success();
    }

    @Operation(summary = "查询车辆详情")
    @GetMapping("/{vehicleId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<VehicleVO> getById(@PathVariable @NotNull Long vehicleId) {
        return ApiResponse.success(vehicleService.getById(SecurityUtils.currentUser().userId(), vehicleId));
    }

    @Operation(summary = "查询我的车辆")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<VehicleVO>> listMine() {
        return ApiResponse.success(vehicleService.listByCurrentUser(SecurityUtils.currentUser().userId()));
    }

}
