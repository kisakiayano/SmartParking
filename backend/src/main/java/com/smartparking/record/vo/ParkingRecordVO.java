package com.smartparking.record.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "停车记录信息")
public record ParkingRecordVO(
        @Schema(description = "记录 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long recordId,

        @Schema(description = "车辆 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long vehicleId,

        @Schema(description = "车位 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long spaceId,

        @Schema(description = "入场时间")
        LocalDateTime entryTime,

        @Schema(description = "出场时间")
        LocalDateTime exitTime,

        @Schema(description = "停车时长，单位分钟")
        Integer duration,

        @Schema(description = "停车费用")
        BigDecimal fee,

        @Schema(description = "状态")
        String status,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
