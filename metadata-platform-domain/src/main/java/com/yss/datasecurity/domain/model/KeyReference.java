package com.yss.datasecurity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyReference {
    private Long ruleId;
    private String ruleName;
    private String ruleType; // DYNAMIC_MASK / STATIC_MASK
    private LocalDateTime lastExecutedAt;
}
