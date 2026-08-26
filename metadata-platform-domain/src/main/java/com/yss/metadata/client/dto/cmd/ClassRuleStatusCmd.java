package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * 分类规则启停命令（PUT /api/classifications/{id}/status body：{enabled: boolean}）。
 */
@Getter
@Setter
public class ClassRuleStatusCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 是否启用 */
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}
