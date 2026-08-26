package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import com.yss.metadata.domain.lineage.model.LineageConfidence;
import com.yss.metadata.domain.lineage.model.LineageType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 人工补录血缘命令（冻结 OpenAPI POST /api/lineage/manual）。
 *
 * <p>成环返回 CYCLE（409，定位冲突边）；graphVersionToken 不匹配返回
 * CONFLICT（409，恢复路径=重读图谱）。置信度/类型为冻结枚举。</p>
 */
@Getter
@Setter
public class LineageManualCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 上游资产 id */
    @NotBlank(message = "fromAssetId 不能为空")
    private String fromAssetId;

    /** 下游资产 id */
    @NotBlank(message = "toAssetId 不能为空")
    private String toAssetId;

    /** 血缘类型（sql/job/manual） */
    @NotNull(message = "type 不能为空")
    private LineageType type;

    /** 置信度（auto-high/auto-mid/manual-high/low） */
    @NotNull(message = "confidence 不能为空")
    private LineageConfidence confidence;

    /** 备注（补录依据） */
    @Size(max = 512, message = "remark 长度不能超过 512")
    private String remark;

    /** 图版本 token（并发防冲突；可选，不匹配返回 CONFLICT） */
    private String graphVersionToken;
}
