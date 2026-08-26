package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultMaskingPolicyDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "数据分级不能为空")
    private String securityGrade; // L1 / L2 / L3 / L4

    @NotBlank(message = "脱敏算法不能为空")
    private String algorithmType; // NULL_VALUE / MD5 / MASK_FIXED_STAR / NO_MASK

    private String description;
}
