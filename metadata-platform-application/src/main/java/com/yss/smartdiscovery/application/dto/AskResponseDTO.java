package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskResponseDTO implements Serializable {
    private String intentSummary;
    private List<String> extractedEntities;
    private List<String> extractedTerms;
    private List<RecommendedAssetDTO> recommendedAssets;
}
