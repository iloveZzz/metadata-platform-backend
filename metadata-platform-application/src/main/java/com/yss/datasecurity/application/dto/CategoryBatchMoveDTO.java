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
public class CategoryBatchMoveDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "分类ID列表不能为空")
    private List<Long> categoryIds;

    @NotNull(message = "目标目录节点不能为空")
    private Long targetTreeNodeId;
}
