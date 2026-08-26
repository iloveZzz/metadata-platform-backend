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
public class KeyPermissionDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "授权主体类型不能为空")
    private String granteeType; // USER / ROLE

    @NotBlank(message = "主体标识不能为空")
    private String granteeId;

    @NotBlank(message = "主体名称不能为空")
    private String granteeName;

    @NotBlank(message = "权限类型不能为空")
    private String permissionType; // USE / MANAGE
}
