package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRuleTransferOwnerDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "规则ID列表不能为空")
    private List<Long> ruleIds;

    @NotBlank(message = "新负责人账号不能为空")
    private String newOwner;
}
