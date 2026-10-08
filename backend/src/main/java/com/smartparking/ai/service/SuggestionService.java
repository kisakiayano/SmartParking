package com.smartparking.ai.service;

import com.smartparking.ai.dto.SuggestionCreateRequest;
import com.smartparking.ai.dto.SuggestionHandleRequest;
import com.smartparking.ai.dto.SuggestionPageQuery;
import com.smartparking.ai.vo.SuggestionVO;
import com.smartparking.common.response.PageResponse;

public interface SuggestionService {

    SuggestionVO create(Long userId, SuggestionCreateRequest request);

    PageResponse<SuggestionVO> pageMine(Long userId, SuggestionPageQuery query);

    SuggestionVO getMine(Long userId, Long suggestionId);

    PageResponse<SuggestionVO> pageForAdmin(SuggestionPageQuery query);

    SuggestionVO getForAdmin(Long suggestionId);

    SuggestionVO handle(Long handlerId, Long suggestionId, SuggestionHandleRequest request);
}
