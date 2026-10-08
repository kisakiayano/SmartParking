package com.smartparking.parking.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "车位实时状态")
public record ParkingSpaceStatusVO(
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
        Integer version
) {
}
