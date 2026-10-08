package com.smartparking.fee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "计费规则分页查询参数")
public record FeeRulePageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "是否启用", example = "true")
        Boolean enabled,

        @Schema(description = "规则名称")
        String ruleName
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
