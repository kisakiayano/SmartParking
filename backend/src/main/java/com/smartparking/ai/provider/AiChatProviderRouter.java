package com.smartparking.ai.provider;

import com.smartparking.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 提供方路由：按配置选择提供方，外部大模型不可用时自动降级到内置规则引擎。
 */
@Component
public class AiChatProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(AiChatProviderRouter.class);

    private final Map<String, AiChatProvider> providers;
    private final AiProperties properties;

    public AiChatProviderRouter(List<AiChatProvider> providerList, AiProperties properties) {
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(AiChatProvider::provider, Function.identity(), (first, second) -> first));
        this.properties = properties;
    }

    public AiChatResult chat(AiChatContext context) {
        AiChatProvider primary = selectProvider();
        try {
            return primary.chat(context);
        } catch (RuntimeException ex) {
            if (RuleBasedAiChatProvider.NAME.equals(primary.provider())) {
                throw ex;
            }
            String reason = ex.getClass().getSimpleName()
                    + (StringUtils.hasText(ex.getMessage()) ? ": " + ex.getMessage() : " (no message)");
            log.warn("AI provider [{}] failed, falling back to [{}]: {}",
                    primary.provider(), RuleBasedAiChatProvider.NAME, reason);
            log.debug("AI provider fallback stack trace", ex);
            return fallbackProvider().chat(context);
        }
    }

    @PostConstruct
    void logResolvedProvider() {
        String configured = properties.getProvider();
        String active = activeProvider();
        if (!active.equalsIgnoreCase(configured)) {
            log.warn("AI provider [{}] is unavailable, using [{}] instead", configured, active);
        } else {
            log.info("AI assistant provider resolved: [{}]", active);
        }
    }

    public String activeProvider() {
        return selectProvider().provider();
    }

    private AiChatProvider selectProvider() {
        AiChatProvider configured = providers.get(properties.getProvider());
        if (configured == null || !configured.isAvailable()) {
            return fallbackProvider();
        }
        return configured;
    }

    private AiChatProvider fallbackProvider() {
        AiChatProvider fallback = providers.get(RuleBasedAiChatProvider.NAME);
        if (fallback == null) {
            throw new IllegalStateException("rule-based AI provider is not available");
        }
        return fallback;
    }
}
