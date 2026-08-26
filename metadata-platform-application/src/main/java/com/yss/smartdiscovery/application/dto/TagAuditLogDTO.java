package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagAuditLogDTO implements Serializable {
    private String id;
    private String batchId;
    private String actionType;
    private String actionName;
    private String operator;
    private Integer fieldCount;
    private String status;
    private LocalDateTime createdAt;
}
