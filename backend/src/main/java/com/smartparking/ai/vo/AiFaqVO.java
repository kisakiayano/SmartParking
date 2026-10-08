package com.smartparking.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 助手推荐问题")
public record AiFaqVO(
        @Schema(description = "推荐问题", example = "还有多少空闲车位？")
        String question,

        @Schema(description = "问题分类", example = "车位")
        String category
) {
}
