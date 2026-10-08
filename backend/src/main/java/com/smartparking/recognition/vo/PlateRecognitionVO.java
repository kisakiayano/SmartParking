package com.smartparking.recognition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.smartparking.record.vo.ParkingRecordVO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "车牌识别结果")
public record PlateRecognitionVO(
        @Schema(description = "车牌号")
        String plateNumber,

        @Schema(description = "车牌颜色")
        String plateColor,

        @Schema(description = "置信度")
        BigDecimal confidence,

        @Schema(description = "服务商")
        String provider,

        @Schema(description = "服务商日志 ID")
        String providerLogId,

        @Schema(description = "识别日志 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long logId,

        @Schema(description = "停车记录")
        ParkingRecordVO parkingRecord
) {
}
