package com.smartparking.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.exception.AiProviderException;
import com.smartparking.ai.service.AiPromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Python RAG 服务实现，通过 app.ai.provider=python 启用，失败时由路由降级到规则引擎。
 */
@Component
public class PythonAiChatProvider implements AiChatProvider {

    public static final String NAME = "python";

    private static final String DEFAULT_BASE_URL = "http://localhost:8000";

    private final AiProperties properties;
    private final AiPromptTemplateService promptTemplateService;
    private final RestClient restClient;

    @Autowired
    public PythonAiChatProvider(AiProperties properties, AiPromptTemplateService promptTemplateService) {
        this(properties, promptTemplateService, buildRestClient(properties));
    }

    PythonAiChatProvider(AiProperties properties, AiPromptTemplateService promptTemplateService, RestClient restClient) {
        this.properties = properties;
        this.promptTemplateService = promptTemplateService;
        this.restClient = restClient;
    }

    @Override
    public String provider() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(properties.getPython().getBaseUrl());
    }

    @Override
    public AiChatResult chat(AiChatContext context) {
        if (!isAvailable()) {
            throw new AiProviderException("Python AI provider is enabled but base-url is not configured");
        }

        PythonChatRequest request = new PythonChatRequest(
                context.userId(),
                context.username(),
                context.question(),
                context.intent() == null ? null : context.intent().name(),
                buildHistory(context.history()),
                context.knowledgeSnippets(),
                context.referencedData(),
                promptTemplateService.resolveSystemPrompt(context.intent())
        );

        PythonChatResponse response;
        try {
            response = restClient.post()
                    .uri(resolveChatUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PythonChatResponse.class);
        } catch (RestClientException ex) {
            throw new AiProviderException("Python AI service request failed: " + ex.getMessage(), ex);
        }

        if (response == null || !StringUtils.hasText(response.answer())) {
            throw new AiProviderException("Python AI service returned an empty answer");
        }

        List<String> referencedData = response.referencedData() != null && !response.referencedData().isEmpty()
                ? response.referencedData()
                : context.referencedData();
        return new AiChatResult(response.answer().trim(), referencedData, NAME);
    }

    private List<HistoryItem> buildHistory(List<AiHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<HistoryItem> items = new ArrayList<>(history.size());
        for (AiHistoryMessage message : history) {
            if (message == null || !StringUtils.hasText(message.content())) {
                continue;
            }
            items.add(new HistoryItem(
                    message.role() == null ? "user" : message.role().toLowerCase(java.util.Locale.ROOT),
                    message.content()
            ));
        }
        return items;
    }

    private String resolveChatUri() {
        String baseUrl = StringUtils.hasText(properties.getPython().getBaseUrl())
                ? properties.getPython().getBaseUrl().trim()
                : DEFAULT_BASE_URL;
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl.endsWith("/v1/chat") ? baseUrl : baseUrl + "/v1/chat";
    }

    private static RestClient buildRestClient(AiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(1, properties.getPython().getTimeoutSeconds()) * 1000;
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record PythonChatRequest(
            @JsonProperty("user_id") Long userId,
            String username,
            String question,
            String intent,
            List<HistoryItem> history,
            @JsonProperty("context_snippets") List<String> contextSnippets,
            @JsonProperty("referenced_data") List<String> referencedData,
            @JsonProperty("system_prompt") String systemPrompt
    ) {
    }

    private record HistoryItem(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PythonChatResponse(
            String answer,
            String provider,
            @JsonProperty("referenced_data") List<String> referencedData,
            List<Source> sources
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Source(String title, String snippet) {
        }
    }
}
