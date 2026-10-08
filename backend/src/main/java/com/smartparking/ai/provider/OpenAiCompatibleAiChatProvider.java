package com.smartparking.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.exception.AiProviderException;
import com.smartparking.ai.support.PromptLeakGuard;
import com.smartparking.ai.service.AiPromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenAI 兼容接口实现，可通过 app.ai.provider=openai 启用，失败时由路由降级到规则引擎。
 */
@Component
public class OpenAiCompatibleAiChatProvider implements AiChatProvider {

    public static final String NAME = "openai";

    private static final int MAX_RESPONSE_TOKENS = 600;

    private final AiProperties properties;
    private final AiKnowledgeBase knowledgeBase;
    private final AiPromptTemplateService promptTemplateService;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleAiChatProvider(
            AiProperties properties,
            AiKnowledgeBase knowledgeBase,
            AiPromptTemplateService promptTemplateService
    ) {
        this(properties, knowledgeBase, promptTemplateService, buildRestClient(properties));
    }

    OpenAiCompatibleAiChatProvider(
            AiProperties properties,
            AiKnowledgeBase knowledgeBase,
            AiPromptTemplateService promptTemplateService,
            RestClient restClient
    ) {
        this.properties = properties;
        this.knowledgeBase = knowledgeBase;
        this.promptTemplateService = promptTemplateService;
        this.restClient = restClient;
    }

    @Override
    public String provider() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return properties.isLlmConfigured();
    }

    @Override
    public AiChatResult chat(AiChatContext context) {
        if (!isAvailable()) {
            throw new AiProviderException("OpenAI compatible provider is enabled but api-key is not configured");
        }

        String systemPrompt = buildSystemPrompt(context);
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                buildMessages(context, systemPrompt),
                properties.getTemperature(),
                MAX_RESPONSE_TOKENS
        );
        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri(resolveChatCompletionsUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientException ex) {
            throw new AiProviderException("AI provider request failed: " + ex.getMessage(), ex);
        }

        return new AiChatResult(
                PromptLeakGuard.scrub(extractAnswer(response), systemPrompt, knowledgeBase.fallbackAnswer()),
                context.referencedData(),
                NAME
        );
    }

    private List<Map<String, String>> buildMessages(AiChatContext context, String systemPrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (context.history() != null) {
            context.history().stream()
                    .filter(history -> StringUtils.hasText(history.content()))
                    .forEach(history -> messages.add(Map.of(
                            "role", normalizeHistoryRole(history.role()),
                            "content", history.content()
                    )));
        }
        messages.add(Map.of("role", "user", "content", context.question()));
        return messages;
    }

    private String buildSystemPrompt(AiChatContext context) {
        StringBuilder prompt = new StringBuilder(promptTemplateService.resolveSystemPrompt(context.intent()));
        if (context.knowledgeSnippets() != null && !context.knowledgeSnippets().isEmpty()) {
            prompt.append("\n\n【系统实时数据】\n");
            context.knowledgeSnippets().forEach(snippet -> prompt.append("- ").append(snippet).append('\n'));
        }
        prompt.append("\n【业务知识】\n").append(knowledgeBase.businessKnowledge());
        return prompt.toString();
    }

    private String normalizeHistoryRole(String role) {
        if (role == null) {
            return "user";
        }
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    private String resolveChatCompletionsUri() {
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
                ? properties.getBaseUrl().trim().toLowerCase(Locale.ROOT)
                : "https://api.openai.com/v1";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/chat/completions";
    }

    private String extractAnswer(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiProviderException("AI provider returned an empty response");
        }
        ChatCompletionResponse.Choice choice = response.choices().get(0);
        if (choice == null || choice.message() == null || !StringUtils.hasText(choice.message().content())) {
            throw new AiProviderException("AI provider returned an empty answer");
        }
        return choice.message().content().trim();
    }

    private static RestClient buildRestClient(AiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(1, properties.getTimeoutSeconds()) * 1000;
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private record ChatCompletionRequest(
            String model,
            List<Map<String, String>> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Choice(Message message) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Message(String role, String content) {
        }
    }
}
