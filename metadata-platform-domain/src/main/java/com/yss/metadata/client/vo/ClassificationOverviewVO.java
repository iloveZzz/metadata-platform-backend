package com.yss.metadata.client.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 分级分类概览视图对象（冻结 OpenAPI GET /api/classifications「识别结果 / 规则列表」响应 data）。
 *
 * <p>组合 VO：识别规则 + 识别结果一次返回，前端拆分为规则区/结果区两个表。</p>
 */
@Getter
@Setter
public class ClassificationOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 识别规则列表 */
    private List<ClassRuleVO> rules;

    /** 识别结果列表（候选/已确认/已修正） */
    private List<ClassificationVO> results;
}
