package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingRuleTransferOwnerDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private List<Long> ruleIds;

    @NotBlank(message = "新负责人不能为空")
    private String newOwner;

    private String comment;
}
