package com.smartparking.fee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("fee_rules")
public class FeeRule {

    @TableId(value = "rule_id", type = IdType.ASSIGN_ID)
    private Long ruleId;

    private String ruleName;

    private BigDecimal unitPrice;

    private Integer freeMinute;

    private BigDecimal maxDaily;

    private Boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getFreeMinute() {
        return freeMinute;
    }

    public void setFreeMinute(Integer freeMinute) {
        this.freeMinute = freeMinute;
    }

    public BigDecimal getMaxDaily() {
        return maxDaily;
    }

    public void setMaxDaily(BigDecimal maxDaily) {
        this.maxDaily = maxDaily;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
