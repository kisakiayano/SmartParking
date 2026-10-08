package com.smartparking.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * AI 提供方：rule-based（内置规则引擎，默认）或 openai（OpenAI 兼容接口）。
     */
    private String provider = "rule-based";

    /**
     * OpenAI 兼容接口的基础地址，例如 https://api.openai.com/v1。
     */
    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey = "";

    private String model = "gpt-4o-mini";

    private int timeoutSeconds = 20;

    /**
     * 随问题一起发送的历史消息条数上限。
     */
    private int maxHistoryMessages = 6;

    private double temperature = 0.3D;

    /**
     * Python RAG 服务配置（app.ai.provider=python 时启用）。
     */
    private PythonClient python = new PythonClient();

    public boolean isLlmConfigured() {
        return "openai".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank();
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public PythonClient getPython() {
        return python;
    }

    public void setPython(PythonClient python) {
        this.python = python;
    }

    public static class PythonClient {

        private String baseUrl = "http://localhost:8000";

        private int timeoutSeconds = 30;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
