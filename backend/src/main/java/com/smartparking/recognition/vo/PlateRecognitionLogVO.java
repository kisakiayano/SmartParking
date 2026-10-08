package com.smartparking.recognition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "车牌识别日志")
public record PlateRecognitionLogVO(
        @Schema(description = "日志 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,

        @Schema(description = "停车记录 ID")
        @JsonSerialize(using = ToStringSerializer.class)
        Long recordId,

        @Schema(description = "车牌号")
        String plateNumber,

        @Schema(description = "车牌颜色")
        String plateColor,

        @Schema(description = "置信度")
        BigDecimal confidence,

        @Schema(description = "图片标识")
        String imageUrl,

        @Schema(description = "识别时间")
        LocalDateTime recognitionTime,

        @Schema(description = "摄像头类型")
        String cameraType,

        @Schema(description = "服务商")
        String provider,

        @Schema(description = "服务商日志 ID")
        String providerLogId,

        @Schema(description = "识别结果")
        String result,

        @Schema(description = "错误码")
        String errorCode,

        @Schema(description = "错误信息")
        String errorMessage,

        @Schema(description = "创建时间")
        LocalDateTime createTime
) {
}
