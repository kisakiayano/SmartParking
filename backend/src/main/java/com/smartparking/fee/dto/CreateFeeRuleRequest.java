package com.smartparking.fee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "新增计费规则请求")
public record CreateFeeRuleRequest(
        @Schema(description = "规则名称", example = "默认计费规则")
        @NotBlank
        @Size(max = 64)
        String ruleName,

        @Schema(description = "每小时单价", example = "5.00")
        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal unitPrice,

        @Schema(description = "免费时长，单位分钟", example = "15")
        @NotNull
        @Min(0)
        Integer freeMinute,

        @Schema(description = "日封顶金额", example = "60.00")
        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal maxDaily,

        @Schema(description = "是否启用", example = "true")
        @NotNull
        Boolean enabled
) {
}
