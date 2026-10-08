package com.smartparking.ai.controller;

import com.smartparking.ai.dto.AiChatRequest;
import com.smartparking.ai.dto.AiChatSessionQuery;
import com.smartparking.ai.service.AssistantService;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatReplyVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.ai.vo.AiFaqVO;
import com.smartparking.ai.vo.AiStatusVO;
import com.smartparking.common.response.ApiResponse;
import com.smartparking.common.response.PageResponse;
import com.smartparking.security.AuthUser;
import com.smartparking.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "AI 智能助手接口")
@Validated
@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @Operation(summary = "向 AI 助手提问")
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiChatReplyVO> chat(@Valid @RequestBody AiChatRequest request) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(assistantService.chat(authUser.userId(), authUser.username(), request));
    }

    @Operation(summary = "查询推荐问题")
    @GetMapping("/faq")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiFaqVO>> faq() {
        return ApiResponse.success(assistantService.faq());
    }

    @Operation(summary = "查询 AI 提供方运行状态")
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiStatusVO> status() {
        return ApiResponse.success(assistantService.status());
    }

    @Operation(summary = "分页查询我的 AI 会话")
    @GetMapping("/chat-sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<AiChatSessionVO>> sessions(@Valid AiChatSessionQuery query) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(assistantService.sessions(authUser.userId(), query));
    }

    @Operation(summary = "查询会话消息")
    @GetMapping("/chat-sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<AiChatMessageVO>> messages(@PathVariable @NotNull Long sessionId) {
        AuthUser authUser = SecurityUtils.currentUser();
        return ApiResponse.success(assistantService.messages(authUser.userId(), sessionId));
    }

    @Operation(summary = "删除我的 AI 会话")
    @DeleteMapping("/chat-sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteSession(@PathVariable @NotNull Long sessionId) {
        AuthUser authUser = SecurityUtils.currentUser();
        assistantService.deleteSession(authUser.userId(), sessionId);
        return ApiResponse.success();
    }
}
