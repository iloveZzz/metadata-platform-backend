package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchApproveCandidatesDTO implements Serializable {
    @NotEmpty(message = "候选ID列表不能为空")
    private List<String> candidateIds;
}
