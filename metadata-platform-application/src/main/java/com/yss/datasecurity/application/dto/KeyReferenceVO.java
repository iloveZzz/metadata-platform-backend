package com.yss.datasecurity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyReferenceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long ruleId;
    private String ruleName;
    private String ruleType;
    private LocalDateTime lastExecutedAt;
}
