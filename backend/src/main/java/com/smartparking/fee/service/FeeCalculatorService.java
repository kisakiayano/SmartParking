package com.smartparking.fee.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface FeeCalculatorService {

    /**
     * 按自然日拆分计算停车费用：免费时长整个会话只扣一次（从第一天扣），
     * 每日费用 = min(ceil(当日计费分钟 / 60) × unitPrice, maxDaily)，多天费用逐日累加。
     */
    BigDecimal calculate(LocalDateTime entryTime, LocalDateTime exitTime);
}
