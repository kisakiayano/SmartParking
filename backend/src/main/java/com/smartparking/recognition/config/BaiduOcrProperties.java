package com.smartparking.recognition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "baidu.ocr")
public class BaiduOcrProperties {

    private String apiKey;

    private String secretKey;

    private String licensePlateUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate";

    private String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";

    private BigDecimal confidenceThreshold = new BigDecimal("0.8000");

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getLicensePlateUrl() {
        return licensePlateUrl;
    }

    public void setLicensePlateUrl(String licensePlateUrl) {
        this.licensePlateUrl = licensePlateUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }
}
