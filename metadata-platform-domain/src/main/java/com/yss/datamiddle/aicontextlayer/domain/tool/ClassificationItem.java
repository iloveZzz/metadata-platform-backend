package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分级分类查询响应项（契约 3.5）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classificationId;
    private String name;
    private Boolean enabled;
    private String source;
    private LocalDateTime updatedAt;
    private Provenance provenance;
}
