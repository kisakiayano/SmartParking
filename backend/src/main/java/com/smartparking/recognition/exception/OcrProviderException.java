package com.smartparking.recognition.exception;

public class OcrProviderException extends RuntimeException {

    private final String errorCode;

    private final String providerLogId;

    public OcrProviderException(String errorCode, String message, String providerLogId) {
        super(message);
        this.errorCode = errorCode;
        this.providerLogId = providerLogId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProviderLogId() {
        return providerLogId;
    }
}
