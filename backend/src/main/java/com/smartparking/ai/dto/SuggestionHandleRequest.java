package com.smartparking.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "管理员处理用户建议请求")
public record SuggestionHandleRequest(
        @Schema(description = "目标状态", example = "RESOLVED")
        @NotBlank(message = "status must not be blank")
        @Pattern(regexp = "^(PROCESSING|RESOLVED|REJECTED)$",
                message = "status must be PROCESSING, RESOLVED or REJECTED")
        String status,

        @Schema(description = "处理回复，标记为已解决或已驳回时必填", example = "已记录需求，将在下个版本评估。")
        @Size(max = 1000, message = "reply length must not exceed 1000")
        String reply
) {
}
