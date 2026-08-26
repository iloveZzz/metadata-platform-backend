package com.yss.datamiddle.semantic.synonym.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 同义词智能推荐项值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynonymRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 推荐的同义候选词
     */
    private String candidateWord;

    /**
     * 匹配相似度得分 (0.0 ~ 1.0)
     */
    private double similarityScore;

    /**
     * 推荐理由类型 (EXACT_CONTAIN, PREFIX_MATCH, SUFFIX_MATCH, EDIT_DISTANCE)
     */
    private String matchReason;

    /**
     * 来源词所属集合 ID（若已存在）
     */
    private String sourceSetId;
}
