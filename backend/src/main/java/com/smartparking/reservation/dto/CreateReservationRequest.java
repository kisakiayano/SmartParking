package com.smartparking.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "创建预约请求")
public record CreateReservationRequest(
        @Schema(description = "车辆 ID", example = "1001")
        @NotNull
        Long vehicleId,

        @Schema(description = "车位 ID", example = "2001")
        @NotNull
        Long spaceId,

        @Schema(description = "预计到达时间", example = "2026-06-08T10:30:00")
        @NotNull
        @Future
        LocalDateTime arriveTime
) {
}
