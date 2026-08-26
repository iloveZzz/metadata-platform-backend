package com.yss.datamiddle.semantic.client.vo;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SynonymRecommendationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String candidateWord;
    private Double similarityScore;
    private String matchReason;
}
