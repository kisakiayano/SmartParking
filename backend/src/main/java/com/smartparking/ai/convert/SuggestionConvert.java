package com.smartparking.ai.convert;

import com.smartparking.ai.entity.UserSuggestion;
import com.smartparking.ai.vo.SuggestionVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SuggestionConvert {

    SuggestionVO toVO(UserSuggestion suggestion);

    List<SuggestionVO> toVOList(List<UserSuggestion> suggestions);
}
