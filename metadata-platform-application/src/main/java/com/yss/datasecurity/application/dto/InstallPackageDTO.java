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
public class InstallPackageDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    private String projectName;

    @NotBlank(message = "算法包版本不能为空")
    private String packageVersion;

    private String engineType;

    private List<String> authorizedFunctions;
}
