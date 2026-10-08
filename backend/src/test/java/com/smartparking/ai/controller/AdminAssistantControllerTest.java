package com.smartparking.ai.controller;

import com.smartparking.ai.service.AssistantService;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatSessionVO;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAssistantController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private AssistantService assistantService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldPageAllSessionsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(assistantService.pageSessionsForAdmin(any()))
                .thenReturn(new PageResponse<>(List.of(sessionVO()), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/ai/chat-sessions?pageNum=1&pageSize=10")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("还有多少空闲车位？"));
    }

    @Test
    void shouldReturnAllSessionMessagesForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(assistantService.messagesForAdmin(100L)).thenReturn(List.of(messageVO()));

        mockMvc.perform(get("/api/v1/admin/ai/chat-sessions/100/messages")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("当前空闲 4 个"));
    }

    @Test
    void shouldRejectUserOnAdminAiEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/ai/chat-sessions")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
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

    private AiChatSessionVO sessionVO() {
        return new AiChatSessionVO(100L, 10L, "还有多少空闲车位？", "rule-based", 2,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private AiChatMessageVO messageVO() {
        return new AiChatMessageVO(1L, 100L, "ASSISTANT", "当前空闲 4 个", "SPACE_STATUS", "rule-based",
                List.of("空闲：4"), LocalDateTime.now());
    }
}
