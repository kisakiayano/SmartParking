package com.smartparking.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartparking.ai.convert.SuggestionConvert;
import com.smartparking.ai.dto.SuggestionCreateRequest;
import com.smartparking.ai.dto.SuggestionHandleRequest;
import com.smartparking.ai.dto.SuggestionPageQuery;
import com.smartparking.ai.entity.UserSuggestion;
import com.smartparking.ai.enums.SuggestionStatus;
import com.smartparking.ai.mapper.UserSuggestionMapper;
import com.smartparking.ai.service.impl.SuggestionServiceImpl;
import com.smartparking.ai.vo.SuggestionVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceImplTest {

    @Mock
    private UserSuggestionMapper userSuggestionMapper;

    @Mock
    private SuggestionConvert suggestionConvert;

    @InjectMocks
    private SuggestionServiceImpl suggestionService;

    @Test
    void shouldCreatePendingSuggestion() {
        when(suggestionConvert.toVO(any())).thenReturn(suggestionVO("PENDING", null));

        suggestionService.create(10L, new SuggestionCreateRequest(
                "FUNCTION", "  希望支持微信支付  ", "出场缴费时希望增加微信扫码支付。", " "));

        ArgumentCaptor<UserSuggestion> captor = ArgumentCaptor.forClass(UserSuggestion.class);
        verify(userSuggestionMapper).insert(captor.capture());
        UserSuggestion inserted = captor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10L);
        assertThat(inserted.getTitle()).isEqualTo("希望支持微信支付");
        assertThat(inserted.getContact()).isNull();
        assertThat(inserted.getStatus()).isEqualTo(SuggestionStatus.PENDING.name());
        assertThat(inserted.getCreateTime()).isNotNull();
    }

    @Test
    void shouldPageMySuggestions() {
        Page<UserSuggestion> page = new Page<>(1, 10);
        page.setRecords(List.of(suggestion(1L, 10L, "PENDING")));
        page.setTotal(1);
        when(userSuggestionMapper.selectPage(any(), any())).thenReturn(page);
        when(suggestionConvert.toVOList(any())).thenReturn(List.of(suggestionVO("PENDING", null)));

        PageResponse<SuggestionVO> result = suggestionService.pageMine(10L,
                new SuggestionPageQuery(1L, 10L, "PENDING", null, null));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
    }

    @Test
    void shouldStartProcessingPendingSuggestion() {
        UserSuggestion suggestion = suggestion(1L, 10L, "PENDING");
        when(userSuggestionMapper.selectById(1L)).thenReturn(suggestion);
        when(suggestionConvert.toVO(any())).thenReturn(suggestionVO("PROCESSING", null));

        suggestionService.handle(1L, 1L, new SuggestionHandleRequest("PROCESSING", null));

        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.PROCESSING.name());
        assertThat(suggestion.getHandlerId()).isEqualTo(1L);
        assertThat(suggestion.getHandleTime()).isNotNull();
        verify(userSuggestionMapper).updateById(suggestion);
    }

    @Test
    void shouldResolveSuggestionWithReply() {
        UserSuggestion suggestion = suggestion(1L, 10L, "PROCESSING");
        when(userSuggestionMapper.selectById(1L)).thenReturn(suggestion);
        when(suggestionConvert.toVO(any())).thenReturn(suggestionVO("RESOLVED", "已记录需求"));

        suggestionService.handle(1L, 1L, new SuggestionHandleRequest("RESOLVED", " 已记录需求 "));

        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.RESOLVED.name());
        assertThat(suggestion.getReply()).isEqualTo("已记录需求");
        assertThat(suggestion.getHandleTime()).isNotNull();
    }

    @Test
    void shouldRejectTerminalStatusWithoutReply() {
        UserSuggestion suggestion = suggestion(1L, 10L, "PENDING");
        when(userSuggestionMapper.selectById(1L)).thenReturn(suggestion);

        BusinessException exception = catchThrowableOfType(
                () -> suggestionService.handle(1L, 1L, new SuggestionHandleRequest("REJECTED", null)),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    void shouldRejectTransitionFromTerminalStatus() {
        UserSuggestion suggestion = suggestion(1L, 10L, "RESOLVED");
        when(userSuggestionMapper.selectById(1L)).thenReturn(suggestion);

        BusinessException exception = catchThrowableOfType(
                () -> suggestionService.handle(1L, 1L, new SuggestionHandleRequest("PROCESSING", null)),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.CONFLICT);
    }

    @Test
    void shouldRejectReadingAnotherUserSuggestion() {
        when(userSuggestionMapper.selectById(1L)).thenReturn(suggestion(1L, 99L, "PENDING"));

        BusinessException exception = catchThrowableOfType(
                () -> suggestionService.getMine(10L, 1L),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.FORBIDDEN);
    }

    @Test
    void shouldRejectUnknownSuggestion() {
        when(userSuggestionMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = catchThrowableOfType(
                () -> suggestionService.getForAdmin(1L),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    private UserSuggestion suggestion(Long suggestionId, Long userId, String status) {
        UserSuggestion suggestion = new UserSuggestion();
        suggestion.setSuggestionId(suggestionId);
        suggestion.setUserId(userId);
        suggestion.setCategory("FUNCTION");
        suggestion.setTitle("希望支持微信支付");
        suggestion.setContent("出场缴费时希望增加微信扫码支付。");
        suggestion.setStatus(status);
        suggestion.setCreateTime(LocalDateTime.now());
        suggestion.setUpdateTime(LocalDateTime.now());
        return suggestion;
    }

    private SuggestionVO suggestionVO(String status, String reply) {
        return new SuggestionVO(1L, 10L, "FUNCTION", "希望支持微信支付", "出场缴费时希望增加微信扫码支付。",
                null, status, reply, null, null, LocalDateTime.now(), LocalDateTime.now());
    }
}
