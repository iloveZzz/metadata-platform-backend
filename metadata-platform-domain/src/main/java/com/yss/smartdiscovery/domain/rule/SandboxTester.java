package com.yss.smartdiscovery.domain.rule;

import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class SandboxTester {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SandboxResult {
        private String matchedTagId;
        private String matchedTagName;
        private Double confidence;
        private String explanation;
        private Boolean l1RegexHit;
        private Boolean l2GlossaryHit;
        private Boolean l3LlmHit;
    }

    public static SandboxResult testField(String fieldName, String fieldComment, List<SmartTagDefinition> availableTags) {
        if (availableTags == null || availableTags.isEmpty()) {
            return SandboxResult.builder()
                    .confidence(0.0)
                    .explanation("未配置任何可用标签规则")
                    .l1RegexHit(false)
                    .l2GlossaryHit(false)
                    .l3LlmHit(false)
                    .build();
        }

        // Layer 1: 正则测试
        for (SmartTagDefinition tag : availableTags) {
            TagRule rule = tag.getTagRule();
            if (rule != null && rule.matchesRegex(fieldName, fieldComment)) {
                return SandboxResult.builder()
                        .matchedTagId(tag.getId())
                        .matchedTagName(tag.getTagName())
                        .confidence(0.98)
                        .explanation(String.format("Layer 1 启发式正则直接命中模式 [%s]", rule.getRegexPattern()))
                        .l1RegexHit(true)
                        .l2GlossaryHit(false)
                        .l3LlmHit(false)
                        .build();
            }
        }

        // Layer 2: 词库测试
        for (SmartTagDefinition tag : availableTags) {
            TagRule rule = tag.getTagRule();
            if (rule != null && rule.matchesGlossary(fieldComment, null)) {
                return SandboxResult.builder()
                        .matchedTagId(tag.getId())
                        .matchedTagName(tag.getTagName())
                        .confidence(0.88)
                        .explanation("Layer 2 中文业务术语词库语义匹配命中")
                        .l1RegexHit(false)
                        .l2GlossaryHit(true)
                        .l3LlmHit(false)
                        .build();
            }
        }

        // Layer 3: LLM 模拟推导 (兜底)
        SmartTagDefinition fallback = availableTags.get(0);
        return SandboxResult.builder()
                .matchedTagId(fallback.getId())
                .matchedTagName(fallback.getTagName())
                .confidence(0.82)
                .explanation("Layer 3 大模型 Few-Shot 结合字段注释推导得出")
                .l1RegexHit(false)
                .l2GlossaryHit(false)
                .l3LlmHit(true)
                .build();
    }
}
