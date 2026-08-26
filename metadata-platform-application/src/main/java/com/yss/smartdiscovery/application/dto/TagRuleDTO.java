package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRuleDTO implements Serializable {
    private String tagId;
    private String regexPattern;
    private List<String> boundTermIds;
    private List<String> boundTermNames;
    private String fewShotPrompt;
    private String scopeFilter;
}
