package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 标记/解除全链路数据存疑命令 DTO (PUT /api/assets/{id}/taint-status)
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaintStatusCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 存疑状态：NORMAL / TAINTED */
    @NotBlank(message = "taintStatus 不能为空")
    @Pattern(regexp = "^(NORMAL|TAINTED)$", message = "taintStatus 只能为 NORMAL 或 TAINTED")
    private String taintStatus;

    /** 变更原因或备注 */
    private String reason;
}
