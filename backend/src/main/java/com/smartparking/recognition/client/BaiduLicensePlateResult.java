package com.smartparking.recognition.client;

import java.math.BigDecimal;

public record BaiduLicensePlateResult(
        String plateNumber,
        String plateColor,
        BigDecimal confidence,
        String providerLogId
) {
}
