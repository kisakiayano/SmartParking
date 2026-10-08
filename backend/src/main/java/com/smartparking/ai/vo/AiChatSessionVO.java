package com.smartparking.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI 助手会话信息")
public record AiChatSessionVO(
        @Schema(description = "会话 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long sessionId,

        @Schema(description = "所属用户 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,

        @Schema(description = "会话标题")
        String title,

        @Schema(description = "实际使用的 AI 提供方")
        String provider,

        @Schema(description = "消息数量")
        Integer messageCount,

        @Schema(description = "最近消息时间")
        LocalDateTime lastMessageTime,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
