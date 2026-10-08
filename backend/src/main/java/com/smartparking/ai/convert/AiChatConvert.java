package com.smartparking.ai.convert;

import com.smartparking.ai.entity.AiChatMessage;
import com.smartparking.ai.entity.AiChatSession;
import com.smartparking.ai.vo.AiChatMessageVO;
import com.smartparking.ai.vo.AiChatSessionVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiChatConvert {

    AiChatSessionVO toSessionVO(AiChatSession session);

    List<AiChatSessionVO> toSessionVOList(List<AiChatSession> sessions);

    default AiChatMessageVO toMessageVO(AiChatMessage message, List<String> referencedData) {
        return new AiChatMessageVO(
                message.getMessageId(),
                message.getSessionId(),
                message.getRole(),
                message.getContent(),
                message.getIntent(),
                message.getProvider(),
                referencedData,
                message.getCreateTime()
        );
    }
}
