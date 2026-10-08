package com.smartparking.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Dashboard 统计信息")
public record DashboardStatisticsVO(
        @Schema(description = "今日收入")
        BigDecimal todayIncome,

        @Schema(description = "今日停车次数")
        Long todayParkingCount,

        @Schema(description = "车位总数")
        Long totalSpaces,

        @Schema(description = "空闲车位数")
        Long freeSpaces,

        @Schema(description = "预约车位数")
        Long reservedSpaces,

        @Schema(description = "占用车位数")
        Long occupiedSpaces,

        @Schema(description = "当前车位占用率，百分比")
        BigDecimal currentOccupancyRate,

        @Schema(description = "最近 7 天收入趋势")
        List<TrendPointVO> revenueTrend7Days,

        @Schema(description = "最近 30 天收入趋势")
        List<TrendPointVO> revenueTrend30Days,

        @Schema(description = "最近 7 天车流量趋势")
        List<TrendPointVO> trafficTrend7Days,

        @Schema(description = "最近 30 天车流量趋势")
        List<TrendPointVO> trafficTrend30Days
) {
}
