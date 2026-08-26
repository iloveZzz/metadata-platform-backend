package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 根因故障节点 VO
 *
 * @author ai
 * @since 2026-08-15
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RootCauseNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String assetId;
    private String assetName;
    private String title;
    private String domain;
    private Integer healthScore;
    private String qualityBand;
    private String taintStatus;
    private String ruleName;
    private String actualMetric;
    private String threshold;
    private String faultTime;
    private Integer distance;
}
