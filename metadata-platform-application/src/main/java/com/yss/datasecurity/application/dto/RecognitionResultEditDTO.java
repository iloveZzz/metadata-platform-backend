package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultEditDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "请指定需要操作的记录ID集合")
    private List<Long> ids;

    private Long categoryId;
    private String recognitionMethod; // AUTO / MANUAL
    private Boolean syncMaskingStatus; // 是否同步开启脱敏生效
    private Boolean isLocked;
}
