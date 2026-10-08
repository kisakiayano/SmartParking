package com.smartparking.vehicle.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.vehicle.dto.VehiclePageQuery;
import com.smartparking.vehicle.service.VehicleService;
import com.smartparking.vehicle.vo.VehicleVO;
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

import java.util.List;

@Tag(name = "管理员车辆接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/vehicles")
@SecurityRequirement(name = "bearerAuth")
public class AdminVehicleController {

    private final VehicleService vehicleService;

    public AdminVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Operation(summary = "管理员分页查询车辆")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<VehicleVO>> pageForAdmin(@Valid VehiclePageQuery query) {
        return ApiResponse.success(vehicleService.pageForAdmin(query));
    }

    @Operation(summary = "管理员根据用户查询车辆")
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<VehicleVO>> listByUserId(@PathVariable @NotNull Long userId) {
        return ApiResponse.success(vehicleService.listByUserId(userId));
    }
}
