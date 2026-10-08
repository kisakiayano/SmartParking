package com.smartparking.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "停车记录分页查询参数")
public record ParkingRecordPageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "停车状态", example = "PARKING")
        @Pattern(regexp = "^$|^(PARKING|FINISHED)$", message = "status must be PARKING or FINISHED")
        String status,

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
