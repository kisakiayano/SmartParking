package com.smartparking.record.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import com.smartparking.security.SecurityUtils;
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

@Tag(name = "停车记录接口")
@Validated
@RestController
@RequestMapping("/api/v1/parking-records")
@SecurityRequirement(name = "bearerAuth")
public class ParkingRecordController {

    private final ParkingRecordService parkingRecordService;

    public ParkingRecordController(ParkingRecordService parkingRecordService) {
        this.parkingRecordService = parkingRecordService;
    }

    @Operation(summary = "查询停车记录详情")
    @GetMapping("/{recordId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ParkingRecordVO> getById(@PathVariable @NotNull Long recordId) {
        return ApiResponse.success(parkingRecordService.getMineById(SecurityUtils.currentUser().userId(), recordId));
    }

    @Operation(summary = "查询个人停车记录")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<ParkingRecordVO>> pageMine(@Valid ParkingRecordPageQuery query) {
        return ApiResponse.success(parkingRecordService.pageMine(SecurityUtils.currentUser().userId(), query));
    }
}
