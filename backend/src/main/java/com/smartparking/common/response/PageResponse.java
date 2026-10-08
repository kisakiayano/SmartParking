package com.smartparking.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页响应")
public record PageResponse<T>(
        @Schema(description = "记录列表")
        List<T> records,

        @Schema(description = "总数")
        long total,

        @Schema(description = "页码")
        long pageNum,

        @Schema(description = "每页数量")
        long pageSize
) {
}
