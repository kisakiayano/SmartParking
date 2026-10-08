package com.smartparking.recognition.service.impl;

import com.smartparking.recognition.model.PlateRecognitionResult;
import com.smartparking.recognition.service.PlateRecognitionService;

public class AliyunPlateRecognitionService implements PlateRecognitionService {

    @Override
    public String provider() {
        return "ALIYUN";
    }

    @Override
    public PlateRecognitionResult recognize(byte[] imageBytes) {
        return PlateRecognitionResult.failed(provider(), null, "NOT_IMPLEMENTED", "AliyunOCR is reserved but not implemented");
    }
}
