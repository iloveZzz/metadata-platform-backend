package com.yss.metadata.domain.governance.model;

import com.yss.metadata.domain.governance.exception.ClassificationStateConflictException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 分级分类结果（数据架构 Classification：asset_id/column_id/name/level/source/status）。
 *
 * <p>状态机：待确认（自动识别候选）→ 已确认 / 已修正（治理专员）。
 * 确认幂等：已确认重复确认无操作；修正以新分类名覆盖并流转为已修正。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Classification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（UUID） */
    private String id;

    /** 资产 id（分类所属资产；传播源资产） */
    private String assetId;

    /** 列 id（列级分类时非空） */
    private String columnId;

    /** 分类名（如 敏感-PII / 内部 / 受限） */
    private String name;

    /** 敏感等级（如 PII / 敏感 / 内部） */
    private String level;

    /** 来源（auto/manual） */
    private String source;

    /** 状态（pending/confirmed/corrected） */
    private ClassificationStatus status;

    /**
     * 确认候选分类（幂等：已确认/已修正重复确认无操作，交互说明「确认幂等」）。
     */
    public void confirm() {
        if (status == ClassificationStatus.PENDING) {
            this.status = ClassificationStatus.CONFIRMED;
        }
    }

    /**
     * 修正候选分类（治理专员改分类名；流转为已修正，覆盖旧名）。
     *
     * @param correctedName 修正后的分类名（非空白）
     */
    public void correct(String correctedName) {
        if (correctedName == null || correctedName.trim().isEmpty()) {
            throw new ClassificationStateConflictException("修正分类名不能为空");
        }
        this.name = correctedName.trim();
        this.status = ClassificationStatus.CORRECTED;
    }
}
