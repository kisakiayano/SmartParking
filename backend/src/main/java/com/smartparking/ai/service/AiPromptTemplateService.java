package com.smartparking.ai.service;

import com.smartparking.ai.dto.UpdateAiPromptTemplateRequest;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.vo.AiPromptTemplateVO;

import java.util.List;

public interface AiPromptTemplateService {

    List<AiPromptTemplateVO> list();

    AiPromptTemplateVO getById(Long templateId);

    AiPromptTemplateVO update(Long templateId, UpdateAiPromptTemplateRequest request);

    /**
     * 解析指定业务意图应使用的系统提示词，找不到配置时返回内置默认提示词。
     */
    String resolveSystemPrompt(AiIntent intent);
}
