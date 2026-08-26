package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveRecordCalibrateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "打标记录ID不能为空")
    private Long recordId;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotNull(message = "分级ID不能为空")
    private Long securityGradeId;

    private Boolean lockPermanent;
}
