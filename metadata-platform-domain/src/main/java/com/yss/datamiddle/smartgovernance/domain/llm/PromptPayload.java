package com.yss.datamiddle.smartgovernance.domain.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 严格元数据白名单 Prompt Payload (严禁任何数据行)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptPayload {
    private String databaseName;
    private String tableName;
    private String tableComment;
    private String columnName;
    private String columnComment;
    private String dataType;
    private List<String> neighborColumnNames;
    private String standardTemplateCode;
}
