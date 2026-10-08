package com.smartparking.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.ai.dto.UpdateAiPromptTemplateRequest;
import com.smartparking.ai.service.AiPromptTemplateService;
import com.smartparking.ai.vo.AiPromptTemplateVO;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAiPromptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
class AdminAiPromptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TokenStateService tokenStateService;

    @MockBean
    private AiPromptTemplateService aiPromptTemplateService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldListPromptsForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(aiPromptTemplateService.list()).thenReturn(List.of(promptVO()));

        mockMvc.perform(get("/api/v1/admin/ai/prompts")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].templateId").value("2001"))
                .andExpect(jsonPath("$.data[0].templateCode").value("default_assistant"));
    }

    @Test
    void shouldUpdatePromptForAdmin() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(aiPromptTemplateService.update(eq(2001L), any(UpdateAiPromptTemplateRequest.class)))
                .thenReturn(promptVO());

        UpdateAiPromptTemplateRequest request =
                new UpdateAiPromptTemplateRequest("通用助手提示词", "请只用实时数据回答。", true);

        mockMvc.perform(put("/api/v1/admin/ai/prompts/2001")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("default_assistant"))
                .andExpect(jsonPath("$.data.systemPrompt").value("请只用实时数据回答。"));
    }

    @Test
    void shouldRejectUserOnAdminPromptEndpoint() throws Exception {
        when(jwtService.parseClaims("user-token")).thenReturn(claims("user01", "USER", 10L));

        mockMvc.perform(get("/api/v1/admin/ai/prompts")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldRejectAnonymousPromptEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/prompts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn404WhenTemplateMissing() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));
        when(aiPromptTemplateService.getById(999L))
                .thenThrow(new BusinessException(ResultCode.NOT_FOUND, "AI prompt template not found"));

        mockMvc.perform(get("/api/v1/admin/ai/prompts/999")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldRejectInvalidUpdateBody() throws Exception {
        when(jwtService.parseClaims("admin-token")).thenReturn(claims("admin", "ADMIN", 1L));

        UpdateAiPromptTemplateRequest request =
                new UpdateAiPromptTemplateRequest("", "", null);

        mockMvc.perform(put("/api/v1/admin/ai/prompts/1")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(aiPromptTemplateService);
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

    private AiPromptTemplateVO promptVO() {
        return new AiPromptTemplateVO(2001L, "default_assistant", "通用助手提示词", null,
                "请只用实时数据回答。", true, LocalDateTime.now(), LocalDateTime.now());
    }
}
