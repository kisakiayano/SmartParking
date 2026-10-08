package com.smartparking.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.ai.config.AiProperties;
import com.smartparking.ai.convert.AiChatConvert;
import com.smartparking.ai.dto.AiChatRequest;
import com.smartparking.ai.dto.AiChatSessionQuery;
import com.smartparking.ai.entity.AiChatMessage;
import com.smartparking.ai.entity.AiChatSession;
import com.smartparking.ai.enums.AiChatRole;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.mapper.AiChatMessageMapper;
import com.smartparking.ai.mapper.AiChatSessionMapper;
import com.smartparking.ai.provider.AiChatContext;
import com.smartparking.ai.provider.AiChatProviderRouter;
import com.smartparking.ai.provider.AiChatResult;
import com.smartparking.ai.provider.AiContextPayload;
import com.smartparking.ai.provider.AiHistoryMessage;
import com.smartparking.ai.provider.AiIntentResolver;
import com.smartparking.ai.provider.AiKnowledgeBase;
import com.smartparking.ai.service.AssistantService;
import com.smartparking.ai.support.AiReferencedDataCodec;
import com.smartparking.ai.support.AssistantContextBuilder;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatReplyVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import com.smartparking.ai.vo.AiFaqVO;
import com.smartparking.ai.vo.AiStatusVO;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssistantServiceImpl implements AssistantService {

    private static final int TITLE_MAX_LENGTH = 40;

    private final AiChatSessionMapper aiChatSessionMapper;
    private final AiChatMessageMapper aiChatMessageMapper;
    private final AiChatConvert aiChatConvert;
    private final AiIntentResolver aiIntentResolver;
    private final AssistantContextBuilder assistantContextBuilder;
    private final AiChatProviderRouter aiChatProviderRouter;
    private final AiKnowledgeBase aiKnowledgeBase;
    private final AiReferencedDataCodec aiReferencedDataCodec;
    private final AiProperties aiProperties;

    public AssistantServiceImpl(
            AiChatSessionMapper aiChatSessionMapper,
            AiChatMessageMapper aiChatMessageMapper,
            AiChatConvert aiChatConvert,
            AiIntentResolver aiIntentResolver,
            AssistantContextBuilder assistantContextBuilder,
            AiChatProviderRouter aiChatProviderRouter,
            AiKnowledgeBase aiKnowledgeBase,
            AiReferencedDataCodec aiReferencedDataCodec,
            AiProperties aiProperties
    ) {
        this.aiChatSessionMapper = aiChatSessionMapper;
        this.aiChatMessageMapper = aiChatMessageMapper;
        this.aiChatConvert = aiChatConvert;
        this.aiIntentResolver = aiIntentResolver;
        this.assistantContextBuilder = assistantContextBuilder;
        this.aiChatProviderRouter = aiChatProviderRouter;
        this.aiKnowledgeBase = aiKnowledgeBase;
        this.aiReferencedDataCodec = aiReferencedDataCodec;
        this.aiProperties = aiProperties;
    }

    @Override
    @Transactional
    public AiChatReplyVO chat(Long userId, String username, AiChatRequest request) {
        String question = request.question().trim();
        AiChatSession session = request.sessionId() == null
                ? createSession(userId, question)
                : requireOwnedSession(userId, request.sessionId());

        AiIntent intent = aiIntentResolver.resolve(question);
        AiContextPayload payload = assistantContextBuilder.build(userId, intent);
        AiChatContext context = new AiChatContext(
                userId,
                username,
                question,
                intent,
                payload.answers(),
                payload.referencedData(),
                loadHistory(session.getSessionId())
        );
        AiChatResult result = aiChatProviderRouter.chat(context);

        LocalDateTime now = LocalDateTime.now();
        aiChatMessageMapper.insert(buildMessage(session.getSessionId(), userId, AiChatRole.USER,
                question, intent, null, List.of(), now));
        aiChatMessageMapper.insert(buildMessage(session.getSessionId(), userId, AiChatRole.ASSISTANT,
                result.answer(), intent, result.provider(), result.referencedData(), now));

        session.setProvider(result.provider());
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setLastMessageTime(now);
        session.setUpdateTime(now);
        aiChatSessionMapper.updateById(session);

        List<String> referencedData = result.referencedData() == null ? List.of() : result.referencedData();
        return new AiChatReplyVO(session.getSessionId(), result.answer(), intent.name(),
                result.provider(), referencedData, now);
    }

    @Override
    public List<AiFaqVO> faq() {
        return aiKnowledgeBase.faqList();
    }

    @Override
    public AiStatusVO status() {
        return new AiStatusVO(
                aiProperties.getProvider(),
                aiChatProviderRouter.activeProvider(),
                true,
                aiProperties.isLlmConfigured(),
                StringUtils.hasText(aiProperties.getPython().getBaseUrl()),
                aiProperties.getPython().getBaseUrl(),
                aiProperties.getBaseUrl(),
                aiProperties.getModel()
        );
    }

    @Override
    public PageResponse<AiChatSessionVO> sessions(Long userId, AiChatSessionQuery query) {
        AiChatSessionQuery normalizedQuery = normalize(query);
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getUpdateTime)
                .orderByDesc(AiChatSession::getSessionId);
        IPage<AiChatSession> page = aiChatSessionMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                aiChatConvert.toSessionVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    public List<AiChatMessageVO> messages(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        return listMessages(sessionId);
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        aiChatMessageMapper.delete(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId));
        aiChatSessionMapper.deleteById(sessionId);
    }

    @Override
    public PageResponse<AiChatSessionVO> pageSessionsForAdmin(AiChatSessionQuery query) {
        AiChatSessionQuery normalizedQuery = normalize(query);
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<>();
        if (normalizedQuery.userId() != null) {
            wrapper.eq(AiChatSession::getUserId, normalizedQuery.userId());
        }
        wrapper.orderByDesc(AiChatSession::getUpdateTime).orderByDesc(AiChatSession::getSessionId);
        IPage<AiChatSession> page = aiChatSessionMapper.selectPage(
                Page.of(normalizedQuery.normalizedPageNum(), normalizedQuery.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                aiChatConvert.toSessionVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    public List<AiChatMessageVO> messagesForAdmin(Long sessionId) {
        requireSession(sessionId);
        return listMessages(sessionId);
    }

    private AiChatSession createSession(Long userId, String question) {
        LocalDateTime now = LocalDateTime.now();
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(buildTitle(question));
        session.setProvider(aiChatProviderRouter.activeProvider());
        session.setMessageCount(0);
        session.setLastMessageTime(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        aiChatSessionMapper.insert(session);
        return session;
    }

    private String buildTitle(String question) {
        if (question.length() <= TITLE_MAX_LENGTH) {
            return question;
        }
        return question.substring(0, TITLE_MAX_LENGTH) + "...";
    }

    private List<AiHistoryMessage> loadHistory(Long sessionId) {
        int limit = Math.max(0, aiProperties.getMaxHistoryMessages());
        if (limit == 0) {
            return List.of();
        }
        IPage<AiChatMessage> page = aiChatMessageMapper.selectPage(
                Page.of(1L, limit),
                new LambdaQueryWrapper<AiChatMessage>()
                        .eq(AiChatMessage::getSessionId, sessionId)
                        .orderByDesc(AiChatMessage::getCreateTime)
                        .orderByDesc(AiChatMessage::getMessageId)
        );
        List<AiChatMessage> records = page.getRecords();
        List<AiHistoryMessage> history = new ArrayList<>(records.size());
        for (int index = records.size() - 1; index >= 0; index--) {
            AiChatMessage message = records.get(index);
            history.add(new AiHistoryMessage(message.getRole(), message.getContent()));
        }
        return history;
    }

    private List<AiChatMessageVO> listMessages(Long sessionId) {
        List<AiChatMessage> messages = aiChatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByAsc(AiChatMessage::getCreateTime)
                .orderByAsc(AiChatMessage::getMessageId));
        return messages.stream()
                .map(message -> aiChatConvert.toMessageVO(message, aiReferencedDataCodec.decode(message.getReferencedData())))
                .toList();
    }

    private AiChatMessage buildMessage(
            Long sessionId,
            Long userId,
            AiChatRole role,
            String content,
            AiIntent intent,
            String provider,
            List<String> referencedData,
            LocalDateTime now
    ) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role.name());
        message.setContent(content);
        message.setIntent(intent == null ? null : intent.name());
        message.setProvider(provider);
        message.setReferencedData(aiReferencedDataCodec.encode(referencedData));
        message.setCreateTime(now);
        return message;
    }

    private AiChatSession requireOwnedSession(Long userId, Long sessionId) {
        AiChatSession session = requireSession(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot access another user's AI session");
        }
        return session;
    }

    private AiChatSession requireSession(Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI session not found");
        }
        return session;
    }

    private AiChatSessionQuery normalize(AiChatSessionQuery query) {
        return query == null ? new AiChatSessionQuery(null, null, null) : query;
    }
}
