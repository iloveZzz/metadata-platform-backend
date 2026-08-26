package com.yss.smartdiscovery.domain.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartTagAuditLog {
    private String id;
    private String batchId;
    private String actionType; // AUTO_APPLY, MANUAL_APPROVE, REJECT, ROLLBACK
    private String actionName;
    private String operator;
    private Integer fieldCount;
    private String status; // APPLIED, ROLLED_BACK
    private LocalDateTime createdAt;

    public void rollback() {
        this.status = "ROLLED_BACK";
    }
}
