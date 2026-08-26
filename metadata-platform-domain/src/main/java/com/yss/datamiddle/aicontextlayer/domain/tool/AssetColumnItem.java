package com.yss.datamiddle.aicontextlayer.domain.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 资产列元数据（经过敏感信息剥离 SEC-04）。
 *
 * <p>禁止包含列注释（comment）、样例值（sampleValue）及敏感描述（description）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetColumnItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String dataType;
    private Boolean primaryKey;
    private Boolean nullable;
    private String classificationLevel;
}
