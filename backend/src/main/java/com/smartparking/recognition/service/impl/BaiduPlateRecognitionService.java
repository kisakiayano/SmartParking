package com.smartparking.recognition.service.impl;

import com.smartparking.recognition.client.BaiduLicensePlateResult;
import com.smartparking.recognition.client.BaiduOcrClient;
import com.smartparking.recognition.config.BaiduOcrProperties;
import com.smartparking.recognition.exception.OcrProviderException;
import com.smartparking.recognition.model.PlateRecognitionResult;
import com.smartparking.recognition.service.PlateRecognitionService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Primary
@Service
public class BaiduPlateRecognitionService implements PlateRecognitionService {

    private static final String PROVIDER = "BAIDU";

    private final BaiduOcrClient baiduOcrClient;
    private final BaiduOcrProperties properties;

    public BaiduPlateRecognitionService(BaiduOcrClient baiduOcrClient, BaiduOcrProperties properties) {
        this.baiduOcrClient = baiduOcrClient;
        this.properties = properties;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public PlateRecognitionResult recognize(byte[] imageBytes) {
        try {
            BaiduLicensePlateResult result = baiduOcrClient.recognizeLicensePlate(imageBytes);
            BigDecimal confidence = result.confidence();
            return PlateRecognitionResult.success(
                    result.plateNumber(),
                    result.plateColor(),
                    confidence,
                    PROVIDER,
                    result.providerLogId(),
                    confidence.compareTo(properties.getConfidenceThreshold()) >= 0
            );
        } catch (OcrProviderException ex) {
            return PlateRecognitionResult.failed(PROVIDER, ex.getProviderLogId(), ex.getErrorCode(), ex.getMessage());
        }
    }
}
