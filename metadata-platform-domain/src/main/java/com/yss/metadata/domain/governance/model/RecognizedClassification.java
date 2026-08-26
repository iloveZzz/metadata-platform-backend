package com.yss.metadata.domain.governance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 识别候选（识别引擎输出：分类名/敏感等级；入库为待确认候选）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognizedClassification implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类名（如 敏感-PII / 敏感） */
    private String name;

    /** 敏感等级（如 PII / 敏感） */
    private String level;
}
