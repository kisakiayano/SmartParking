package com.smartparking.ai.provider;

import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.exception.AiProviderException;
import com.smartparking.ai.service.AiPromptTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PythonAiChatProviderTest {

    private MockRestServiceServer server;
    private AiPromptTemplateService promptTemplateService;
    private PythonAiChatProvider provider;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.getPython().setBaseUrl("http://localhost:8000");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        promptTemplateService = mock(AiPromptTemplateService.class);
        provider = new PythonAiChatProvider(properties, promptTemplateService, builder.build());
    }

    @Test
    void shouldPostRequestAndMapResponse() {
        when(promptTemplateService.resolveSystemPrompt(AiIntent.SPACE_STATUS)).thenReturn("SYSTEM-PROMPT");
        server.expect(requestTo("http://localhost:8000/v1/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.user_id").value(10))
                .andExpect(jsonPath("$.username").value("user01"))
                .andExpect(jsonPath("$.intent").value("SPACE_STATUS"))
                .andExpect(jsonPath("$.system_prompt").value("SYSTEM-PROMPT"))
                .andExpect(jsonPath("$.history[0].role").value("user"))
                .andRespond(withSuccess(
                        "{\"answer\":\"Python 回答\",\"provider\":\"python-rag\","
                                + "\"referenced_data\":[\"空闲：4\"]}",
                        MediaType.APPLICATION_JSON));

        AiChatResult result = provider.chat(context());

        assertThat(result.answer()).isEqualTo("Python 回答");
        assertThat(result.referencedData()).containsExactly("空闲：4");
        assertThat(result.provider()).isEqualTo(PythonAiChatProvider.NAME);
        server.verify();
    }

    @Test
    void shouldThrowProviderExceptionOnNon2xxResponse() {
        when(promptTemplateService.resolveSystemPrompt(any())).thenReturn("SYSTEM-PROMPT");
        server.expect(requestTo("http://localhost:8000/v1/chat"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.chat(context()))
                .isInstanceOf(AiProviderException.class);
        server.verify();
    }

    private AiChatContext context() {
        return new AiChatContext(
                10L,
                "user01",
                "还有多少空闲车位？",
                AiIntent.SPACE_STATUS,
                List.of("当前空闲 4 个"),
                List.of("空闲：4"),
                List.of(new AiHistoryMessage("USER", "你好"))
        );
    }
}
