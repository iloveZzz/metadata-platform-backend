package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * 切换采集任务生效状态命令。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorStatusCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 生效状态（true 启用 / false 停用） */
    @NotNull(message = "生效状态不能为空")
    private Boolean enabled;
}
