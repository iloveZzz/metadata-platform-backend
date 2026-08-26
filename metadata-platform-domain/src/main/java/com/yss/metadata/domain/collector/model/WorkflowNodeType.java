package com.yss.metadata.domain.collector.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流节点类型。
 */
@Getter
@AllArgsConstructor
public enum WorkflowNodeType {

    DLINK("dlink", "Dlink 采集计算节点"),
    JDBC_PROBE("jdbc_probe", "JDBC 连通探测节点"),
    SCHEMA_PARSE("schema_parse", "元数据解析节点"),
    CATALOG_INGEST("catalog_ingest", "资产编目入库节点");

    @JsonValue
    private final String code;
    private final String description;

    public static WorkflowNodeType fromCode(String code) {
        if (code == null) return null;
        for (WorkflowNodeType type : values()) {
            if (type.code.equalsIgnoreCase(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
