package com.smartparking.reservation.controller;

import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.reservation.dto.CreateReservationRequest;
import com.smartparking.reservation.dto.ReservationPageQuery;
import com.smartparking.reservation.service.ReservationService;
import com.smartparking.reservation.vo.ReservationVO;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "预约接口")
@Validated
@RestController
@RequestMapping("/api/v1/reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "创建预约")
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReservationVO> create(@Valid @RequestBody CreateReservationRequest request) {
        return ApiResponse.success(reservationService.create(SecurityUtils.currentUser().userId(), request));
    }

    @Operation(summary = "查询预约详情")
    @GetMapping("/{reservationId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReservationVO> getById(@PathVariable @NotNull Long reservationId) {
        return ApiResponse.success(reservationService.getById(SecurityUtils.currentUser().userId(), reservationId));
    }

    @Operation(summary = "查看个人预约记录")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<ReservationVO>> pageMine(@Valid ReservationPageQuery query) {
        return ApiResponse.success(reservationService.pageMine(SecurityUtils.currentUser().userId(), query));
    }

    @Operation(summary = "取消预约")
    @PutMapping("/{reservationId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> cancel(@PathVariable @NotNull Long reservationId) {
        reservationService.cancel(SecurityUtils.currentUser().userId(), reservationId);
        return ApiResponse.success();
    }
}
