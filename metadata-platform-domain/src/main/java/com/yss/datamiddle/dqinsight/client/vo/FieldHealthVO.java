package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.HealthBand;
import com.yss.datamiddle.dqinsight.domain.model.HealthState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 字段级健康分（冻结 OpenAPI FieldHealth；低分字段标红置顶由前端按 lowScore / score 排序处理）。
 */
@Getter
@Setter
@NoArgsConstructor
public class FieldHealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 字段名 */
    private String fieldName;

    /** 状态 */
    private HealthState state;

    /** 健康分 0~100；无结果时为 null */
    private Integer score;

    /** 档位；无结果 / 过期为 null */
    private HealthBand band;

    /** 过期态字段（独立展示态） */
    private boolean expired;

    /** 规则数量 */
    private Integer ruleCount;

    /** 低分字段（score &lt; 75 = 差档，档位阈值 OQ-01 已确认） */
    private boolean lowScore;
}
