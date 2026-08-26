package com.yss.smartdiscovery.application.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagCandidateDTO implements Serializable {
    private String id;
    private String tableName;
    private String columnName;
    private String columnComment;
    private String currentTag;
    private String recommendedTagId;
    private String recommendedTagName;
    private String tagCategory;
    private String source;
    private Double confidence;
    private String inferenceReason;
    private String status;
    private LocalDateTime createdAt;
}
