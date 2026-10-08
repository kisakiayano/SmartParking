package com.smartparking.recognition.model;

import java.math.BigDecimal;

public record PlateRecognitionResult(
        boolean recognized,
        boolean confidenceQualified,
        String plateNumber,
        String plateColor,
        BigDecimal confidence,
        String provider,
        String providerLogId,
        String errorCode,
        String errorMessage
) {
    public static PlateRecognitionResult success(
            String plateNumber,
            String plateColor,
            BigDecimal confidence,
            String provider,
            String providerLogId,
            boolean confidenceQualified
    ) {
        return new PlateRecognitionResult(true, confidenceQualified, plateNumber, plateColor, confidence, provider, providerLogId, null, null);
    }

    public static PlateRecognitionResult failed(String provider, String providerLogId, String errorCode, String errorMessage) {
        return new PlateRecognitionResult(false, false, null, null, null, provider, providerLogId, errorCode, errorMessage);
    }
}
