package com.smartparking.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI 助手回答")
public record AiChatReplyVO(
        @Schema(description = "会话 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long sessionId,

        @Schema(description = "回答内容")
        String answer,

        @Schema(description = "识别出的业务意图")
        String intent,

        @Schema(description = "实际使用的 AI 提供方")
        String provider,

        @Schema(description = "回答引用的实时业务数据")
        List<String> referencedData,

        @Schema(description = "回答时间")
        LocalDateTime createTime
) {
}
