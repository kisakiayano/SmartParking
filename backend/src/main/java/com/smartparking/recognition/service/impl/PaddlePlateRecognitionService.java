package com.smartparking.recognition.service.impl;

import com.smartparking.recognition.model.PlateRecognitionResult;
import com.smartparking.recognition.service.PlateRecognitionService;

public class PaddlePlateRecognitionService implements PlateRecognitionService {

    @Override
    public String provider() {
        return "PADDLE";
    }

    @Override
    public PlateRecognitionResult recognize(byte[] imageBytes) {
        return PlateRecognitionResult.failed(provider(), null, "NOT_IMPLEMENTED", "PaddleOCR is reserved but not implemented");
    }
}
