package com.smartparking.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Schema(description = "用户分页查询参数")
public record UserPageQuery(
        @Schema(description = "页码", example = "1")
        @Min(1)
        Long pageNum,

        @Schema(description = "每页数量", example = "10")
        @Min(1)
        Long pageSize,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "姓名")
        String name,

        @Schema(description = "手机号")
        String phone,

        @Schema(description = "角色", example = "USER")
        @Pattern(regexp = "^$|^(ADMIN|USER)$", message = "role must be ADMIN or USER")
        String role,

        @Schema(description = "状态", example = "ENABLED")
        @Pattern(regexp = "^$|^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
        String status
) {
    public long normalizedPageNum() {
        return pageNum == null ? 1L : pageNum;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10L : Math.min(pageSize, 100L);
    }
}
