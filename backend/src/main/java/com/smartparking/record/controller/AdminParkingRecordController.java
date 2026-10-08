package com.smartparking.record.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.record.dto.ManualEntryRequest;
import com.smartparking.record.dto.ManualExitRequest;
import com.smartparking.record.dto.ParkingRecordPageQuery;
import com.smartparking.record.service.ParkingRecordService;
import com.smartparking.record.vo.ParkingRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员停车记录接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/parking-records")
@SecurityRequirement(name = "bearerAuth")
public class AdminParkingRecordController {

    private final ParkingRecordService parkingRecordService;

    public AdminParkingRecordController(ParkingRecordService parkingRecordService) {
        this.parkingRecordService = parkingRecordService;
    }

    @Operation(summary = "人工登记入场")
    @PostMapping("/entry")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParkingRecordVO> manualEntry(@Valid @RequestBody ManualEntryRequest request) {
        return ApiResponse.success(parkingRecordService.manualEntry(request));
    }

    @Operation(summary = "人工登记出场")
    @PostMapping("/exit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ParkingRecordVO> manualExit(@Valid @RequestBody ManualExitRequest request) {
        return ApiResponse.success(parkingRecordService.manualExit(request));
    }

    @Operation(summary = "管理员停车记录分页查询")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ParkingRecordVO>> pageForAdmin(@Valid ParkingRecordPageQuery query) {
        return ApiResponse.success(parkingRecordService.pageForAdmin(query));
    }
}
