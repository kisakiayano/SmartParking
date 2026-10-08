package com.smartparking.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "预约分页查询参数")
public record ReservationPageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "预约状态", example = "ACTIVE")
        @Pattern(regexp = "^$|^(ACTIVE|USED|EXPIRED|CANCELLED)$", message = "status must be ACTIVE, USED, EXPIRED or CANCELLED")
        String status,

        @Schema(description = "用户 ID")
        Long userId,

        @Schema(description = "车辆 ID")
        Long vehicleId,

        @Schema(description = "车位 ID")
        Long spaceId
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
