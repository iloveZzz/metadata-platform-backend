package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCategoryCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 512, message = "分类名称不能超过512字符")
    private String categoryName;

    @Size(max = 128, message = "分类缩写不能超过128字符")
    private String categoryCode;

    @NotNull(message = "所属目录节点不能为空")
    private Long treeNodeId;

    @NotNull(message = "关联数据分级不能为空")
    private Long securityGradeId;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级最高为1")
    @Max(value = 5, message = "优先级最低为5")
    private Integer priority;

    private List<String> recognitionFeatures;

    private Object scanDimensionConfig;

    @Size(max = 2048, message = "描述不能超过2048字符")
    private String description;
}
