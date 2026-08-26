package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleBatchRunDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private List<Long> ruleIds; // 为空表示按 ruleScope 跑
    private String ruleScope; // ENABLED_ONLY / ALL_RULES
    private Boolean lineageInheritanceEnabled; // 是否开启基于血缘自动继承
}
