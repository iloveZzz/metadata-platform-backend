package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchModifyCandidatesDTO implements Serializable {
    @NotEmpty(message = "候选ID列表不能为空")
    private List<String> candidateIds;
    @NotBlank(message = "目标安全等级不能为空")
    private String targetLevel;
    private String targetSensitiveType;
    private String reason;
}
