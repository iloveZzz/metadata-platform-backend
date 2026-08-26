package com.yss.datamiddle.semantic.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long assetId;
    private String level;
    private String columnName;
    private String semanticType;
    private Long semanticId;
    private String status;
    private String releasedBy;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
}
