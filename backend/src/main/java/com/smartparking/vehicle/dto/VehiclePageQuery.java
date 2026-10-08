package com.smartparking.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "车辆分页查询参数")
public record VehiclePageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "车牌号")
        String plateNumber,

        @Schema(description = "车辆类型")
        String vehicleType,

        @Schema(description = "用户 ID")
        Long userId,

        @Schema(description = "排序字段")
        @Pattern(regexp = "^$|^(createTime|updateTime|plateNumber)$", message = "unsupported sort field")
        String sortBy,

        @Schema(description = "排序方向")
        @Pattern(regexp = "^$|^(asc|desc)$", message = "order must be asc or desc")
        String order
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
