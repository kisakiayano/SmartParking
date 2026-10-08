package com.smartparking.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "用户建议信息")
public record SuggestionVO(
        @Schema(description = "建议 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long suggestionId,

        @Schema(description = "提交用户 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,

        @Schema(description = "建议分类")
        String category,

        @Schema(description = "标题")
        String title,

        @Schema(description = "建议内容")
        String content,

        @Schema(description = "联系方式")
        String contact,

        @Schema(description = "处理状态")
        String status,

        @Schema(description = "管理员回复")
        String reply,

        @Schema(description = "处理人 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long handlerId,

        @Schema(description = "处理时间")
        LocalDateTime handleTime,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
