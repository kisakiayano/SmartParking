package com.smartparking.ai.convert;

import com.smartparking.ai.entity.AiPromptTemplate;
import com.smartparking.ai.vo.AiPromptTemplateVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiPromptTemplateConvert {

    AiPromptTemplateVO toVO(AiPromptTemplate template);

    List<AiPromptTemplateVO> toVOList(List<AiPromptTemplate> templates);
}
