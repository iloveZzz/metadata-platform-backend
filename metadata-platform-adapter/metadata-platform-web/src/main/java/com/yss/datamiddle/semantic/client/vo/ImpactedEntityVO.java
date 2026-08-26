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
public class ImpactedEntityVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long entityId;
    private String entityType;
    private String entityName;
    private String owner;
    private String impactDescription;
}
