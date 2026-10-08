package com.smartparking.reservation.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "预约信息")
public record ReservationVO(
        @Schema(description = "预约 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long reservationId,

        @Schema(description = "用户 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long userId,

        @Schema(description = "车辆 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long vehicleId,

        @Schema(description = "车位 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long spaceId,

        @Schema(description = "预约提交时间")
        LocalDateTime reserveTime,

        @Schema(description = "预计到达时间")
        LocalDateTime arriveTime,

        @Schema(description = "预约状态")
        String status,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
