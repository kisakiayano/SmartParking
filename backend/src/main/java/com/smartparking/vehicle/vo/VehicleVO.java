package com.smartparking.vehicle.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "车辆信息")
public record VehicleVO(
        @Schema(description = "车辆 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long vehicleId,

        @Schema(description = "车牌号")
        String plateNumber,

        @Schema(description = "车辆类型")
        String vehicleType,

        @Schema(description = "所属用户 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
