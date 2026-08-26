package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNodeCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    private Long parentId;

    @NotBlank(message = "目录节点名称不能为空")
    @Size(max = 128, message = "目录节点名称长度不能超过128字符")
    private String nodeName;

    @Builder.Default
    private String visibility = "PUBLIC";

    private List<String> admins;

    @Size(max = 512, message = "目录描述不能超过512字符")
    private String description;
}
