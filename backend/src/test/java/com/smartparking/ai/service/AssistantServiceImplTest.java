package com.smartparking.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.convert.AiChatConvert;
import com.smartparking.ai.dto.AiChatRequest;
import com.smartparking.ai.dto.AiChatSessionQuery;
import com.smartparking.ai.entity.AiChatMessage;
import com.smartparking.ai.entity.AiChatSession;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.mapper.AiChatMessageMapper;
import com.smartparking.ai.mapper.AiChatSessionMapper;
import com.smartparking.ai.provider.AiChatContext;
import com.smartparking.ai.provider.AiChatProviderRouter;
import com.smartparking.ai.provider.AiChatResult;
import com.smartparking.ai.provider.AiContextPayload;
import com.smartparking.ai.provider.AiIntentResolver;
import com.smartparking.ai.provider.AiKnowledgeBase;
import com.smartparking.ai.service.impl.AssistantServiceImpl;
import com.smartparking.ai.support.AiReferencedDataCodec;
import com.smartparking.ai.support.AssistantContextBuilder;
import com.smartparking.ai.vo.AiChatReplyVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock
    private AiChatSessionMapper aiChatSessionMapper;

    @Mock
    private AiChatMessageMapper aiChatMessageMapper;

    @Mock
    private AiChatConvert aiChatConvert;

    @Mock
    private AiIntentResolver aiIntentResolver;

    @Mock
    private AssistantContextBuilder assistantContextBuilder;

    @Mock
    private AiChatProviderRouter aiChatProviderRouter;

    @Mock
    private AiKnowledgeBase aiKnowledgeBase;

    @Mock
    private AiReferencedDataCodec aiReferencedDataCodec;

    @Mock
    private AiProperties aiProperties;

    @InjectMocks
    private AssistantServiceImpl assistantService;

    @Test
    void shouldCreateSessionAndPersistMessages() {
        when(aiChatProviderRouter.activeProvider()).thenReturn("rule-based");
        when(aiChatSessionMapper.insert(any(AiChatSession.class))).thenAnswer(invocation -> {
            AiChatSession session = invocation.getArgument(0);
            session.setSessionId(100L);
            return 1;
        });
        when(aiProperties.getMaxHistoryMessages()).thenReturn(6);
        when(aiChatMessageMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 6));
        when(aiIntentResolver.resolve("还有多少空闲车位？")).thenReturn(AiIntent.SPACE_STATUS);
        when(assistantContextBuilder.build(10L, AiIntent.SPACE_STATUS))
                .thenReturn(new AiContextPayload(List.of("当前空闲 4 个"), List.of("空闲：4")));
        when(aiChatProviderRouter.chat(any(AiChatContext.class)))
                .thenReturn(new AiChatResult("当前空闲 4 个", List.of("空闲：4"), "rule-based"));
        when(aiReferencedDataCodec.encode(any())).thenReturn("[]");

        AiChatReplyVO reply = assistantService.chat(10L, "user01", new AiChatRequest(null, "还有多少空闲车位？"));

        assertThat(reply.sessionId()).isEqualTo(100L);
        assertThat(reply.answer()).isEqualTo("当前空闲 4 个");
        assertThat(reply.intent()).isEqualTo(AiIntent.SPACE_STATUS.name());
        assertThat(reply.provider()).isEqualTo("rule-based");
        assertThat(reply.referencedData()).containsExactly("空闲：4");

        ArgumentCaptor<AiChatSession> sessionCaptor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(aiChatSessionMapper).insert(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(sessionCaptor.getValue().getTitle()).isEqualTo("还有多少空闲车位？");
        assertThat(sessionCaptor.getValue().getCreateTime()).isNotNull();

        ArgumentCaptor<AiChatSession> updateCaptor = ArgumentCaptor.forClass(AiChatSession.class);
        verify(aiChatSessionMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getMessageCount()).isEqualTo(2);

        verify(aiChatMessageMapper, times(2)).insert(any(AiChatMessage.class));
    }

    @Test
    void shouldReuseOwnedSession() {
        AiChatSession existingSession = session(100L, 10L, 2);
        when(aiChatSessionMapper.selectById(100L)).thenReturn(existingSession);
        when(aiProperties.getMaxHistoryMessages()).thenReturn(6);
        when(aiChatMessageMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 6));
        when(aiIntentResolver.resolve("停车费用怎么算？")).thenReturn(AiIntent.FEE_RULE);
        when(assistantContextBuilder.build(10L, AiIntent.FEE_RULE)).thenReturn(AiContextPayload.empty());
        when(aiChatProviderRouter.chat(any(AiChatContext.class)))
                .thenReturn(new AiChatResult("每小时 5 元", List.of(), "rule-based"));
        when(aiReferencedDataCodec.encode(any())).thenReturn("[]");

        AiChatReplyVO reply = assistantService.chat(10L, "user01", new AiChatRequest(100L, "停车费用怎么算？"));

        assertThat(reply.sessionId()).isEqualTo(100L);
        verify(aiChatSessionMapper, never()).insert(any(AiChatSession.class));
        verify(aiChatSessionMapper).updateById(existingSession);
        assertThat(existingSession.getMessageCount()).isEqualTo(4);
    }

    @Test
    void shouldRejectSessionOwnedByAnotherUser() {
        when(aiChatSessionMapper.selectById(100L)).thenReturn(session(100L, 99L, 0));

        BusinessException exception = catchThrowableOfType(
                () -> assistantService.chat(10L, "user01", new AiChatRequest(100L, "你好")),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.FORBIDDEN);
        verify(aiChatMessageMapper, never()).insert(any(AiChatMessage.class));
    }

    @Test
    void shouldRejectUnknownSession() {
        when(aiChatSessionMapper.selectById(100L)).thenReturn(null);

        BusinessException exception = catchThrowableOfType(
                () -> assistantService.chat(10L, "user01", new AiChatRequest(100L, "你好")),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void shouldPageUserSessions() {
        Page<AiChatSession> page = new Page<>(1, 10);
        page.setRecords(List.of(session(100L, 10L, 2)));
        page.setTotal(1);
        when(aiChatSessionMapper.selectPage(any(), any())).thenReturn(page);
        when(aiChatConvert.toSessionVOList(any())).thenReturn(List.of(sessionVO()));

        PageResponse<AiChatSessionVO> result = assistantService.sessions(10L, new AiChatSessionQuery(1L, 10L, null));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).title()).isEqualTo("还有多少空闲车位？");
    }

    @Test
    void shouldDeleteOwnedSessionWithMessages() {
        when(aiChatSessionMapper.selectById(100L)).thenReturn(session(100L, 10L, 2));

        assistantService.deleteSession(10L, 100L);

        verify(aiChatMessageMapper).delete(any());
        verify(aiChatSessionMapper).deleteById(100L);
    }

    @Test
    void shouldRejectDeletingAnotherUserSession() {
        when(aiChatSessionMapper.selectById(100L)).thenReturn(session(100L, 99L, 2));

        BusinessException exception = catchThrowableOfType(
                () -> assistantService.deleteSession(10L, 100L),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.FORBIDDEN);
        verify(aiChatSessionMapper, never()).deleteById(anyLong());
    }

    private AiChatSession session(Long sessionId, Long userId, int messageCount) {
        AiChatSession session = new AiChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle("还有多少空闲车位？");
        session.setProvider("rule-based");
        session.setMessageCount(messageCount);
        session.setLastMessageTime(LocalDateTime.now());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    private AiChatSessionVO sessionVO() {
        return new AiChatSessionVO(100L, 10L, "还有多少空闲车位？", "rule-based", 2,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}
