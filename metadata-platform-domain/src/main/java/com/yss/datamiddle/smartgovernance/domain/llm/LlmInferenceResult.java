package com.yss.datamiddle.smartgovernance.domain.llm;

import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 大模型结构化推理结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmInferenceResult {
    private String sensitiveType;
    private SecurityLevel securityLevel;
    private String clauseRef;
    private String reasoning;
    private BigDecimal confidence;
    private Boolean isDegraded;
}
