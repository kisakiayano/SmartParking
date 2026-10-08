package com.smartparking.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI 助手会话消息")
public record AiChatMessageVO(
        @Schema(description = "消息 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long messageId,

        @Schema(description = "会话 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long sessionId,

        @Schema(description = "消息角色：USER 或 ASSISTANT")
        String role,

        @Schema(description = "消息内容")
        String content,

        @Schema(description = "识别出的业务意图")
        String intent,

        @Schema(description = "实际使用的 AI 提供方")
        String provider,

        @Schema(description = "回答引用的实时业务数据")
        List<String> referencedData,

        @Schema(description = "消息时间")
        LocalDateTime createTime
) {
}
