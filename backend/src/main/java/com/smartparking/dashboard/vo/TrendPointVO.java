package com.smartparking.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "趋势数据点")
public record TrendPointVO(
        @Schema(description = "日期")
        LocalDate date,

        @Schema(description = "数值")
        BigDecimal value
) {
}
