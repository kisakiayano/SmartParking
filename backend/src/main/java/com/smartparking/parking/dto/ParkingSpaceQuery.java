package com.smartparking.parking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "车位查询参数")
public record ParkingSpaceQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "区域", example = "A")
        String area,

        @Schema(description = "车位类型", example = "STANDARD")
        String type,

        @Schema(description = "状态", example = "FREE")
        @Pattern(regexp = "^$|^(FREE|RESERVED|OCCUPIED)$", message = "status must be FREE, RESERVED or OCCUPIED")
        String status
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
