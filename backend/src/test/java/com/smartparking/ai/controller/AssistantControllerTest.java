package com.smartparking.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.ai.dto.AiChatRequest;
import com.smartparking.ai.service.AssistantService;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatReplyVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.ai.vo.AiFaqVO;
import com.smartparking.ai.vo.AiStatusVO;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.JwtAuthenticationEntryPoint;
import com.smartparking.security.JwtAuthenticationFilter;
import com.smartparking.security.JwtService;
import com.smartparking.security.TokenStateService;
import com.smartparking.security.RestAccessDeniedHandler;
import com.smartparking.security.SecurityConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private AssistantService assistantService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldChatForAuthenticatedUser() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(assistantService.chat(eq(10L), eq("user01"), any(AiChatRequest.class))).thenReturn(replyVO());

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiChatRequest(null, "还有多少空闲车位？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("100"))
                .andExpect(jsonPath("$.data.answer").value("当前空闲 4 个"))
                .andExpect(jsonPath("$.data.intent").value("SPACE_STATUS"))
                .andExpect(jsonPath("$.data.referencedData[0]").value("空闲：4"));
    }

    @Test
    void shouldRejectAnonymousChat() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiChatRequest(null, "你好"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldRejectBlankQuestion() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldReturnFaqList() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(assistantService.faq()).thenReturn(List.of(new AiFaqVO("还有多少空闲车位？", "车位")));

        mockMvc.perform(get("/api/v1/ai/faq")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].question").value("还有多少空闲车位？"));
    }

    @Test
    void shouldReturnAiProviderStatus() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(assistantService.status()).thenReturn(new AiStatusVO("python", "python", true, false, true,
                "http://ai-service:8000", "https://api.openai.com/v1", "gpt-4o-mini"));

        mockMvc.perform(get("/api/v1/ai/status")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configuredProvider").value("python"))
                .andExpect(jsonPath("$.data.activeProvider").value("python"))
                .andExpect(jsonPath("$.data.pythonConfigured").value(true));
    }

    @Test
    void shouldPageMySessions() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(assistantService.sessions(eq(10L), any()))
                .thenReturn(new PageResponse<>(List.of(sessionVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/ai/chat-sessions?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void shouldReturnSessionMessages() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));
        when(assistantService.messages(10L, 100L)).thenReturn(List.of(messageVO()));

        mockMvc.perform(get("/api/v1/ai/chat-sessions/100/messages")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("ASSISTANT"));
    }

    @Test
    void shouldDeleteMySession() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(delete("/api/v1/ai/chat-sessions/100")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private Claims claims(String username, String role, Long userId) {
        return Jwts.claims()
                .subject(username)
                .add("userId", userId)
                .add("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .build();
    }

    private AiChatReplyVO replyVO() {
        return new AiChatReplyVO(100L, "当前空闲 4 个", "SPACE_STATUS", "rule-based",
                List.of("空闲：4"), LocalDateTime.now());
    }

    private AiChatSessionVO sessionVO() {
        return new AiChatSessionVO(100L, 10L, "还有多少空闲车位？", "rule-based", 2,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private AiChatMessageVO messageVO() {
        return new AiChatMessageVO(1L, 100L, "ASSISTANT", "当前空闲 4 个", "SPACE_STATUS", "rule-based",
                List.of("空闲：4"), LocalDateTime.now());
    }
}
