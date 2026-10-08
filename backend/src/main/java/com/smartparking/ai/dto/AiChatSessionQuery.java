package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "AI 会话分页查询参数")
public record AiChatSessionQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "用户 ID，仅管理员可用", example = "1001")
        Long userId
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
