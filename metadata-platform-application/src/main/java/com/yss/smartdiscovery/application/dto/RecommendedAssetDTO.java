package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedAssetDTO implements Serializable {
    private String tableName;
    private String tableCnName;
    private Integer matchScore;
    private String recommendReason;
    private String certifiedTerm;
    private Integer dqHealthScore;
    private String dqLevel;
}
