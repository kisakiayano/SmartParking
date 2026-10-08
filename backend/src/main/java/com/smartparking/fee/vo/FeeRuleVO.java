package com.smartparking.fee.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "计费规则信息")
public record FeeRuleVO(
        @Schema(description = "规则 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long ruleId,

        @Schema(description = "规则名称")
        String ruleName,

        @Schema(description = "每小时单价")
        BigDecimal unitPrice,

        @Schema(description = "免费时长，单位分钟")
        Integer freeMinute,

        @Schema(description = "日封顶金额")
        BigDecimal maxDaily,

        @Schema(description = "是否启用")
        Boolean enabled,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
