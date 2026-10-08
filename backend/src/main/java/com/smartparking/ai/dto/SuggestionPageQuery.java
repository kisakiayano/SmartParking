package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "用户建议分页查询参数")
public record SuggestionPageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "处理状态", example = "PENDING")
        @Pattern(regexp = "^$|^(PENDING|PROCESSING|RESOLVED|REJECTED)$",
                message = "status must be PENDING, PROCESSING, RESOLVED or REJECTED")
        String status,

        @Schema(description = "建议分类", example = "FUNCTION")
        @Pattern(regexp = "^$|^(FUNCTION|EXPERIENCE|BUG|CHARGE|OTHER)$",
                message = "category must be FUNCTION, EXPERIENCE, BUG, CHARGE or OTHER")
        String category,

        @Schema(description = "标题或内容关键字")
        @Size(max = 64, message = "keyword length must not exceed 64")
        String keyword
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
