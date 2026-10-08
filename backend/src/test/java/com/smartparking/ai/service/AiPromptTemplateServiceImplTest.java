package com.smartparking.ai.service;

import com.smartparking.ai.convert.AiPromptTemplateConvert;
import com.smartparking.ai.dto.UpdateAiPromptTemplateRequest;
import com.smartparking.ai.entity.AiPromptTemplate;
import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.mapper.AiPromptTemplateMapper;
import com.smartparking.ai.service.impl.AiPromptTemplateServiceImpl;
import com.smartparking.ai.vo.AiPromptTemplateVO;
import com.smartparking.common.exception.BusinessException;
import com.smartparking.common.response.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPromptTemplateServiceImplTest {

    @Mock
    private AiPromptTemplateMapper aiPromptTemplateMapper;

    @Mock
    private AiPromptTemplateConvert aiPromptTemplateConvert;

    @InjectMocks
    private AiPromptTemplateServiceImpl aiPromptTemplateService;

    @Test
    void shouldResolveIntentSpecificEnabledTemplate() {
        AiPromptTemplate template = template(2002L, "space_status", "SPACE_STATUS", "车位咨询提示词", true);
        when(aiPromptTemplateMapper.selectList(any())).thenReturn(List.of(template));

        String prompt = aiPromptTemplateService.resolveSystemPrompt(AiIntent.SPACE_STATUS);

        assertThat(prompt).isEqualTo("车位咨询提示词");
    }

    @Test
    void shouldFallBackToDefaultAssistantTemplate() {
        AiPromptTemplate defaultTemplate = template(2001L, "default_assistant", null, "通用助手提示词", true);
        when(aiPromptTemplateMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(defaultTemplate));

        String prompt = aiPromptTemplateService.resolveSystemPrompt(AiIntent.SPACE_STATUS);

        assertThat(prompt).isEqualTo("通用助手提示词");
    }

    @Test
    void shouldFallBackToBuiltInDefaultPromptWhenMapperReturnsNull() {
        when(aiPromptTemplateMapper.selectList(any())).thenReturn(null);

        String prompt = aiPromptTemplateService.resolveSystemPrompt(AiIntent.FEE_RULE);

        assertThat(prompt).isEqualTo(AiPromptTemplateServiceImpl.DEFAULT_SYSTEM_PROMPT);
    }

    @Test
    void shouldFallBackToBuiltInDefaultPromptWhenDatabaseUnavailable() {
        when(aiPromptTemplateMapper.selectList(any()))
                .thenThrow(new DataAccessResourceFailureException("table ai_prompt_templates does not exist"));

        String prompt = aiPromptTemplateService.resolveSystemPrompt(AiIntent.SPACE_STATUS);

        assertThat(prompt).isEqualTo(AiPromptTemplateServiceImpl.DEFAULT_SYSTEM_PROMPT);
    }

    @Test
    void shouldRejectUpdateForUnknownTemplate() {
        when(aiPromptTemplateMapper.selectById(99L)).thenReturn(null);

        BusinessException exception = catchThrowableOfType(
                () -> aiPromptTemplateService.update(99L, new UpdateAiPromptTemplateRequest("名称", "提示词", true)),
                BusinessException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getResultCode()).isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    void shouldUpdateTemplateFieldsAndTimestamp() {
        AiPromptTemplate template = template(2001L, "default_assistant", null, "旧提示词", true);
        when(aiPromptTemplateMapper.selectById(2001L)).thenReturn(template);
        when(aiPromptTemplateConvert.toVO(template)).thenReturn(vo(2001L));

        aiPromptTemplateService.update(2001L,
                new UpdateAiPromptTemplateRequest("  新名称  ", "  新提示词  ", false));

        assertThat(template.getTemplateName()).isEqualTo("新名称");
        assertThat(template.getSystemPrompt()).isEqualTo("新提示词");
        assertThat(template.getEnabled()).isFalse();
        assertThat(template.getUpdateTime()).isNotNull();
        verify(aiPromptTemplateMapper).updateById(template);
    }

    private AiPromptTemplate template(Long id, String code, String intent, String prompt, boolean enabled) {
        AiPromptTemplate template = new AiPromptTemplate();
        template.setTemplateId(id);
        template.setTemplateCode(code);
        template.setTemplateName(code);
        template.setIntent(intent);
        template.setSystemPrompt(prompt);
        template.setEnabled(enabled);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        return template;
    }

    private AiPromptTemplateVO vo(Long id) {
        return new AiPromptTemplateVO(id, "default_assistant", "通用助手提示词", null, "新提示词", false,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
