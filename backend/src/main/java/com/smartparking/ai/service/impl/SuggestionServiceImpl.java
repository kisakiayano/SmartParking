package com.smartparking.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.ai.convert.SuggestionConvert;
import com.smartparking.ai.dto.SuggestionCreateRequest;
import com.smartparking.ai.dto.SuggestionHandleRequest;
import com.smartparking.ai.dto.SuggestionPageQuery;
import com.smartparking.ai.entity.UserSuggestion;
import com.smartparking.ai.enums.SuggestionStatus;
import com.smartparking.ai.mapper.UserSuggestionMapper;
import com.smartparking.ai.service.SuggestionService;
import com.smartparking.ai.vo.SuggestionVO;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.PageResponse;
import com.smartparking.common.response.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    private final UserSuggestionMapper userSuggestionMapper;
    private final SuggestionConvert suggestionConvert;

    public SuggestionServiceImpl(UserSuggestionMapper userSuggestionMapper, SuggestionConvert suggestionConvert) {
        this.userSuggestionMapper = userSuggestionMapper;
        this.suggestionConvert = suggestionConvert;
    }

    @Override
    @Transactional
    public SuggestionVO create(Long userId, SuggestionCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserSuggestion suggestion = new UserSuggestion();
        suggestion.setUserId(userId);
        suggestion.setCategory(request.category());
        suggestion.setTitle(request.title().trim());
        suggestion.setContent(request.content().trim());
        suggestion.setContact(StringUtils.hasText(request.contact()) ? request.contact().trim() : null);
        suggestion.setStatus(SuggestionStatus.PENDING.name());
        suggestion.setCreateTime(now);
        suggestion.setUpdateTime(now);
        userSuggestionMapper.insert(suggestion);
        return suggestionConvert.toVO(suggestion);
    }

    @Override
    public PageResponse<SuggestionVO> pageMine(Long userId, SuggestionPageQuery query) {
        SuggestionPageQuery normalizedQuery = normalize(query);
        LambdaQueryWrapper<UserSuggestion> wrapper = buildWrapper(normalizedQuery)
                .eq(UserSuggestion::getUserId, userId);
        return toPage(wrapper, normalizedQuery);
    }

    @Override
    public SuggestionVO getMine(Long userId, Long suggestionId) {
        return suggestionConvert.toVO(requireOwnedSuggestion(userId, suggestionId));
    }

    @Override
    public PageResponse<SuggestionVO> pageForAdmin(SuggestionPageQuery query) {
        SuggestionPageQuery normalizedQuery = normalize(query);
        return toPage(buildWrapper(normalizedQuery), normalizedQuery);
    }

    @Override
    public SuggestionVO getForAdmin(Long suggestionId) {
        return suggestionConvert.toVO(requireSuggestion(suggestionId));
    }

    @Override
    @Transactional
    public SuggestionVO handle(Long handlerId, Long suggestionId, SuggestionHandleRequest request) {
        UserSuggestion suggestion = requireSuggestion(suggestionId);
        SuggestionStatus currentStatus = parseStatus(suggestion.getStatus());
        SuggestionStatus targetStatus = parseStatus(request.status());
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "cannot change suggestion status from " + currentStatus.name() + " to " + targetStatus.name());
        }
        if (targetStatus.isTerminal() && !StringUtils.hasText(request.reply())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "reply is required when resolving or rejecting a suggestion");
        }

        LocalDateTime now = LocalDateTime.now();
        suggestion.setStatus(targetStatus.name());
        if (StringUtils.hasText(request.reply())) {
            suggestion.setReply(request.reply().trim());
        }
        suggestion.setHandlerId(handlerId);
        suggestion.setHandleTime(now);
        suggestion.setUpdateTime(now);
        userSuggestionMapper.updateById(suggestion);
        return suggestionConvert.toVO(suggestion);
    }

    private LambdaQueryWrapper<UserSuggestion> buildWrapper(SuggestionPageQuery query) {
        LambdaQueryWrapper<UserSuggestion> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(UserSuggestion::getStatus, query.status());
        }
        if (StringUtils.hasText(query.category())) {
            wrapper.eq(UserSuggestion::getCategory, query.category());
        }
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(condition -> condition
                    .like(UserSuggestion::getTitle, keyword)
                    .or()
                    .like(UserSuggestion::getContent, keyword));
        }
        return wrapper;
    }

    private PageResponse<SuggestionVO> toPage(LambdaQueryWrapper<UserSuggestion> wrapper, SuggestionPageQuery query) {
        wrapper.orderByDesc(UserSuggestion::getCreateTime).orderByDesc(UserSuggestion::getSuggestionId);
        IPage<UserSuggestion> page = userSuggestionMapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()),
                wrapper
        );
        return new PageResponse<>(
                suggestionConvert.toVOList(page.getRecords()),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    private UserSuggestion requireOwnedSuggestion(Long userId, Long suggestionId) {
        UserSuggestion suggestion = requireSuggestion(suggestionId);
        if (!suggestion.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "cannot access another user's suggestion");
        }
        return suggestion;
    }

    private UserSuggestion requireSuggestion(Long suggestionId) {
        UserSuggestion suggestion = userSuggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "suggestion not found");
        }
        return suggestion;
    }

    private SuggestionStatus parseStatus(String status) {
        try {
            return SuggestionStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "invalid suggestion status: " + status);
        }
    }

    private SuggestionPageQuery normalize(SuggestionPageQuery query) {
        return query == null ? new SuggestionPageQuery(null, null, null, null, null) : query;
    }
}
