package com.smartparking.ai.controller;

import com.smartparking.ai.dto.AiChatSessionQuery;
import com.smartparking.ai.service.AssistantService;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员 AI 会话接口")
@Validated
@RestController
@RequestMapping("/api/v1/admin/ai")
@SecurityRequirement(name = "bearerAuth")
public class AdminAssistantController {

    private final AssistantService assistantService;

    public AdminAssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @Operation(summary = "分页查询全部 AI 会话")
    @GetMapping("/chat-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AiChatSessionVO>> sessions(@Valid AiChatSessionQuery query) {
        return ApiResponse.success(assistantService.pageSessionsForAdmin(query));
    }

    @Operation(summary = "查询指定 AI 会话消息")
    @GetMapping("/chat-sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AiChatMessageVO>> messages(@PathVariable @NotNull Long sessionId) {
        return ApiResponse.success(assistantService.messagesForAdmin(sessionId));
    }
}
