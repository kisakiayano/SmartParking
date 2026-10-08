package com.smartparking.parking.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "车位信息")
public record ParkingSpaceVO(
        @Schema(description = "车位 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long spaceId,

        @Schema(description = "区域")
        String area,

        @Schema(description = "车位类型")
        String type,

        @Schema(description = "状态")
        String status,

        @Schema(description = "乐观锁版本号")
        Integer version,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
