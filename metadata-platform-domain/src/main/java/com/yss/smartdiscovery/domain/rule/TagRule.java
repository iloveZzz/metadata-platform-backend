package com.yss.smartdiscovery.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRule {
    private String id;
    private String tagId;
    private String regexPattern;
    private List<String> boundTermIds;
    private List<String> boundTermNames;
    private String fewShotPrompt;
    private String scopeFilter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (regexPattern != null && !regexPattern.trim().isEmpty()) {
            try {
                Pattern.compile(regexPattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Layer 1 正则表达式语法非法: " + e.getMessage());
            }
        }
    }

    public boolean matchesRegex(String fieldName, String fieldComment) {
        if (regexPattern == null || regexPattern.trim().isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        if (fieldName != null && pattern.matcher(fieldName).find()) {
            return true;
        }
        return fieldComment != null && pattern.matcher(fieldComment).find();
    }

    public boolean matchesGlossary(String fieldComment, List<String> businessTerms) {
        if (boundTermNames == null || boundTermNames.isEmpty()) {
            return false;
        }
        for (String term : boundTermNames) {
            if (fieldComment != null && fieldComment.contains(term)) {
                return true;
            }
            if (businessTerms != null && businessTerms.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
