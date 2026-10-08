package com.smartparking.ai.provider;

import com.smartparking.ai.enums.AiIntent;
import com.smartparking.ai.vo.AiFaqVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置停车业务知识库，为规则引擎和外部大模型提供统一的知识与兜底回答。
 */
@Component
public class AiKnowledgeBase {

    private static final List<FaqEntry> FAQ_ENTRIES = List.of(
            new FaqEntry("还有多少空闲车位？", "车位"),
            new FaqEntry("停车费用怎么算？", "费用"),
            new FaqEntry("我登记了哪些车辆？", "车辆"),
            new FaqEntry("我的预约有哪些？", "预约"),
            new FaqEntry("怎么在线预约车位？", "预约流程"),
            new FaqEntry("车辆入场出场流程是什么？", "出入场"),
            new FaqEntry("我想提一个建议。", "意见反馈")
    );

    public List<AiFaqVO> faqList() {
        return FAQ_ENTRIES.stream()
                .map(entry -> new AiFaqVO(entry.question(), entry.category()))
                .toList();
    }

    public String answerFor(AiIntent intent) {
        return switch (intent) {
            case RESERVATION_GUIDE -> "在线预约流程：进入「在线预约」→ 选择空闲车位 → 选择车辆 → 填写预计到达时间 → 提交预约。"
                    + "预约成功后车位会被锁定，超时未使用系统会自动释放，您也可以在「我的预约」中主动取消。";
            case ENTRY_EXIT_GUIDE -> "入场：车辆到达入口后由车牌识别或管理员人工登记，系统分配车位并生成停车记录；"
                    + "出场：在出口识别车牌或由管理员登记出场，系统按计费规则结算费用并释放车位。";
            case SUGGESTION_GUIDE -> "您可以在「意见反馈」页面提交建议：选择分类（功能建议、体验优化、问题反馈、收费相关或其他），"
                    + "填写标题和内容后提交。管理员处理完成后，您可以在该页面查看回复。";
            case SYSTEM_FAQ -> "我可以帮您查询空闲车位、我的车辆、我的预约、停车记录和当前计费规则，"
                    + "也可以引导您完成预约、入场出场和意见反馈。";
            default -> null;
        };
    }

    public String greetingAnswer() {
        return "您好，我是智慧停车助手。我可以在线帮您查询空闲车位、我的车辆、预约、停车记录和计费规则，也欢迎您提出使用建议。";
    }

    public String fallbackAnswer() {
        return "抱歉，我暂时无法准确回答这个问题。您可以换个说法，例如：\n"
                + "• 还有多少空闲车位？\n"
                + "• 我的预约有哪些？\n"
                + "• 停车费用怎么算？\n"
                + "• 车辆入场出场流程是什么？\n"
                + "也可以前往「意见反馈」提交建议，我们会尽快处理。";
    }

    public String followUpHint(AiIntent intent) {
        return switch (intent) {
            case SPACE_STATUS -> "如需锁定车位，可前往「在线预约」提交预约。";
            case MY_VEHICLES -> "您可以在「我的车辆」中新增或维护车牌信息。";
            case MY_RESERVATIONS -> "如需取消，可在「我的预约」中操作。";
            case MY_PARKING_RECORDS -> "完整历史可在「停车记录」中查看。";
            case FEE_RULE -> "最终费用以出场结算为准。";
            default -> null;
        };
    }

    /**
     * 供外部大模型使用的业务知识文本。
     */
    public String businessKnowledge() {
        return String.join("\n",
                answerFor(AiIntent.RESERVATION_GUIDE),
                answerFor(AiIntent.ENTRY_EXIT_GUIDE),
                answerFor(AiIntent.SUGGESTION_GUIDE),
                answerFor(AiIntent.SYSTEM_FAQ));
    }

    private record FaqEntry(String question, String category) {
    }
}
