package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 根因溯源分析报告 VO (GET /api/dq/assets/{id}/root-cause)
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String targetAssetId;
    private RootCauseNodeVO rootAsset;
    private List<PropagationStepVO> propagationPath;
    private String confidence;
    private String summary;
    private List<String> suggestions;
    private String createdAt;
}
