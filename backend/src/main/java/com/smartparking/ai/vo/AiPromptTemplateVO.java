package com.smartparking.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "AI 提示词模板信息")
public record AiPromptTemplateVO(
        @Schema(description = "模板 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long templateId,

        @Schema(description = "模板编码")
        String templateCode,

        @Schema(description = "模板名称")
        String templateName,

        @Schema(description = "关联的业务意图，通用模板为空")
        String intent,

        @Schema(description = "系统提示词内容")
        String systemPrompt,

        @Schema(description = "是否启用")
        Boolean enabled,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
