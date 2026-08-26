package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResultBatchDeleteDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "识别结果ID列表不能为空")
    private List<Long> ids;
}
