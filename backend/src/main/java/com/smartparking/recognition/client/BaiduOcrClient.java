package com.smartparking.recognition.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import com.smartparking.recognition.config.BaiduOcrProperties;
import com.smartparking.recognition.exception.OcrProviderException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Base64;

@Component
public class BaiduOcrClient {

    private static final String TOKEN_CACHE_KEY = "baidu:ocr:access-token";

    private final RestClient restClient;
    private final BaiduOcrProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;

    public BaiduOcrClient(
            RestClient restClient,
            BaiduOcrProperties properties,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public BaiduLicensePlateResult recognizeLicensePlate(byte[] imageBytes) {
        String accessToken = getAccessToken();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("image", Base64.getEncoder().encodeToString(imageBytes));

        JsonNode response = restClient.post()
                .uri(properties.getLicensePlateUrl() + "?access_token={accessToken}", accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new OcrProviderException("BAIDU_EMPTY_RESPONSE", "baidu OCR response is empty", null);
        }
        String providerLogId = response.path("log_id").asText(null);
        if (response.has("error_code")) {
            throw new OcrProviderException(
                    response.path("error_code").asText("BAIDU_ERROR"),
                    response.path("error_msg").asText("baidu OCR request failed"),
                    providerLogId
            );
        }
        JsonNode wordsResult = response.path("words_result");
        String plateNumber = wordsResult.path("number").asText(null);
        if (!StringUtils.hasText(plateNumber)) {
            throw new OcrProviderException("BAIDU_EMPTY_PLATE", "plate number not recognized", providerLogId);
        }
        return new BaiduLicensePlateResult(
                plateNumber,
                wordsResult.path("color").asText(null),
                averageConfidence(wordsResult.path("probability")),
                providerLogId
        );
    }

    private String getAccessToken() {
        validateProperties();
        Object cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cached instanceof String token && StringUtils.hasText(token)) {
            return token;
        }

        JsonNode response = restClient.get()
                .uri(
                        properties.getTokenUrl() + "?grant_type=client_credentials&client_id={apiKey}&client_secret={secretKey}",
                        properties.getApiKey(),
                        properties.getSecretKey()
                )
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new OcrProviderException("BAIDU_TOKEN_EMPTY_RESPONSE", "baidu token response is empty", null);
        }
        if (response.has("error")) {
            throw new OcrProviderException(
                    response.path("error").asText("BAIDU_TOKEN_ERROR"),
                    response.path("error_description").asText("baidu token request failed"),
                    null
            );
        }
        String accessToken = response.path("access_token").asText(null);
        if (!StringUtils.hasText(accessToken)) {
            throw new OcrProviderException("BAIDU_TOKEN_MISSING", "baidu token missing access_token", null);
        }
        long expiresIn = Math.max(60L, response.path("expires_in").asLong(3600L) - 60L);
        redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, accessToken, Duration.ofSeconds(expiresIn));
        return accessToken;
    }

    private void validateProperties() {
        if (!StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "baidu OCR credentials are not configured");
        }
    }

    private BigDecimal averageConfidence(JsonNode probability) {
        if (probability == null || probability.isMissingNode() || !probability.isArray() || probability.size() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (JsonNode node : probability) {
            if (node.isNumber()) {
                total = total.add(node.decimalValue());
                count++;
            } else if (node.has("probability")) {
                total = total.add(node.path("probability").decimalValue());
                count++;
            }
        }
        if (count == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }
}
