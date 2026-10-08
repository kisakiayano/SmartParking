package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 基于关键词的轻量意图识别，保证在没有外部大模型时也能回答常见业务问题。
 */
@Component
public class AiIntentResolver {

    private static final List<String> GREETING_KEYWORDS = List.of("你好", "您好", "在吗", "hello", "hi");
    private static final List<String> SUGGESTION_KEYWORDS = List.of("建议", "反馈", "意见", "投诉");
    private static final List<String> HELP_KEYWORDS = List.of("帮助", "你能做什么", "能做什么", "有什么功能", "help");
    private static final List<String> HOW_KEYWORDS = List.of("怎么", "如何", "流程", "步骤", "规则", "方法");
    private static final List<String> FEE_KEYWORDS = List.of("费用", "收费", "计费", "价格", "多少钱", "费率", "封顶");
    private static final List<String> VEHICLE_KEYWORDS = List.of("车辆", "车牌", "我的车", "车子");
    private static final List<String> SPACE_KEYWORDS = List.of("车位", "空位", "停车位", "空余");
    private static final List<String> RECORD_KEYWORDS = List.of("记录", "停了多久", "停车时长", "停车时间", "历史");
    private static final List<String> RESERVATION_KEYWORDS = List.of("预约");
    private static final List<String> ENTRY_EXIT_KEYWORDS = List.of("入场", "出场", "停车", "缴费", "交费", "结算");

    public AiIntent resolve(String question) {
        if (!StringUtils.hasText(question)) {
            return AiIntent.UNKNOWN;
        }
        String normalized = question.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, GREETING_KEYWORDS)) {
            return AiIntent.GREETING;
        }
        if (containsAny(normalized, SUGGESTION_KEYWORDS)) {
            return AiIntent.SUGGESTION_GUIDE;
        }
        if (containsAny(normalized, HELP_KEYWORDS)) {
            return AiIntent.SYSTEM_FAQ;
        }
        if (asksHow(normalized, RESERVATION_KEYWORDS)) {
            return AiIntent.RESERVATION_GUIDE;
        }
        if (containsAny(normalized, FEE_KEYWORDS)) {
            return AiIntent.FEE_RULE;
        }
        if (asksHow(normalized, ENTRY_EXIT_KEYWORDS)) {
            return AiIntent.ENTRY_EXIT_GUIDE;
        }
        if (containsAny(normalized, RESERVATION_KEYWORDS)) {
            return AiIntent.MY_RESERVATIONS;
        }
        if (asksHow(normalized, SPACE_KEYWORDS)) {
            return AiIntent.SPACE_STATUS;
        }
        if (containsAny(normalized, VEHICLE_KEYWORDS)) {
            return AiIntent.MY_VEHICLES;
        }
        if (containsAny(normalized, SPACE_KEYWORDS)) {
            return AiIntent.SPACE_STATUS;
        }
        if (containsAny(normalized, RECORD_KEYWORDS)) {
            return AiIntent.MY_PARKING_RECORDS;
        }
        return AiIntent.UNKNOWN;
    }

    private boolean asksHow(String question, List<String> topics) {
        return containsAny(question, topics) && containsAny(question, HOW_KEYWORDS);
    }

    private boolean containsAny(String question, List<String> keywords) {
        return keywords.stream().anyMatch(question::contains);
    }
}
