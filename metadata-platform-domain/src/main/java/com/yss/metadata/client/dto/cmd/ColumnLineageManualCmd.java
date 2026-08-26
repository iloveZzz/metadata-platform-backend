package com.yss.metadata.client.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 手工补录字段级血缘命令。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnLineageManualCmd implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 上游资产 ID */
    @NotBlank(message = "fromAssetId 不能为空")
    private String fromAssetId;

    /** 上游字段 ID */
    @NotBlank(message = "fromColumnId 不能为空")
    private String fromColumnId;

    /** 下游资产 ID */
    @NotBlank(message = "toAssetId 不能为空")
    private String toAssetId;

    /** 下游字段 ID */
    @NotBlank(message = "toColumnId 不能为空")
    private String toColumnId;

    /** 字段转换表达式（可选） */
    private String transformExpr;

    /** 表达式类型 (DIRECT/COMPUTED/AGGREGATE/MANUAL) */
    private String exprType;

    /** 备注信息 */
    private String remark;

    /** 图版本 token（用于并发乐观锁校验） */
    private String graphVersionToken;
}
