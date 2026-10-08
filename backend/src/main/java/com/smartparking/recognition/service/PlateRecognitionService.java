package com.smartparking.recognition.service;

import com.smartparking.recognition.model.PlateRecognitionResult;

public interface PlateRecognitionService {

    String provider();

    PlateRecognitionResult recognize(byte[] imageBytes);
}
