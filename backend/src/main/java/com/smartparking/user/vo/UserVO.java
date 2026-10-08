package com.smartparking.user.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户信息")
public record UserVO(
        @Schema(description = "用户 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "姓名")
        String name,

        @Schema(description = "手机号")
        String phone,

        @Schema(description = "角色")
        String role,

        @Schema(description = "状态")
        String status,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
