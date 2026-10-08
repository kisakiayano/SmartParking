package com.smartparking.ai.service;

import com.smartparking.ai.dto.AiChatRequest;
import com.smartparking.ai.dto.AiChatSessionQuery;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatReplyVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.ai.vo.AiFaqVO;
import com.smartparking.ai.vo.AiStatusVO;
import com.smartparking.common.response.PageResponse;

import java.util.List;

public interface AssistantService {

    AiChatReplyVO chat(Long userId, String username, AiChatRequest request);

    AiStatusVO status();

    List<AiFaqVO> faq();

    PageResponse<AiChatSessionVO> sessions(Long userId, AiChatSessionQuery query);

    List<AiChatMessageVO> messages(Long userId, Long sessionId);

    void deleteSession(Long userId, Long sessionId);

    PageResponse<AiChatSessionVO> pageSessionsForAdmin(AiChatSessionQuery query);

    List<AiChatMessageVO> messagesForAdmin(Long sessionId);
}
