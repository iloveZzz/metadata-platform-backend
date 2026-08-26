package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 动态脱敏时效白名单领域实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingWhitelist {
    private Long id;
    private String whitelistName;
    private String granteeType; // USER, ROLE, APP
    private String granteeId;
    private Long categoryId;
    private Long ruleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // ACTIVE, EXPIRED, REVOKED
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 判断指定时间点白名单是否有效 (毫秒级)
     */
    public boolean isEffectiveAt(LocalDateTime targetTime) {
        if (!"ACTIVE".equals(status)) {
            return false;
        }
        if (startTime != null && targetTime.isBefore(startTime)) {
            return false;
        }
        if (endTime != null && targetTime.isAfter(endTime)) {
            return false;
        }
        return true;
    }

    /**
     * 手动撤销/提前终止白名单
     */
    public void revoke() {
        this.status = "REVOKED";
        this.updatedAt = LocalDateTime.now();
    }
}
