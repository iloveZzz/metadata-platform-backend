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
public class CategoryBatchStatusDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "分类ID列表不能为空")
    private List<Long> categoryIds;

    @NotBlank(message = "生效状态不能为空")
    private String status; // ENABLED / DISABLED

    private String disablePolicy; // RETAIN_TAGS / DELETE_TAGS
}
