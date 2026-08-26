package com.yss.metadata.domain.dq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 根因溯源分析报告聚合模型
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private String targetAssetId;
    private RootCauseNode rootAsset;
    private List<PropagationStep> propagationPath;
    private String confidence;
    private String summary;
    private List<String> suggestions;
    private LocalDateTime createdAt;
}
