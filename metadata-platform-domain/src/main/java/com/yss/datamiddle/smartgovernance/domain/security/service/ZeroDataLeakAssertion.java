package com.yss.datamiddle.smartgovernance.domain.security.service;

import com.yss.datamiddle.smartgovernance.domain.llm.PromptPayload;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 运行时元数据安全隔离断言器 (BAC-01 / SEC-01)
 * 严禁传入真实数据样本行，必须通过白名单与敏感特征校验
 */
public class ZeroDataLeakAssertion {

    private static final Pattern SUSPECTED_DATA_ROW_PATTERNS = Pattern.compile(
            "(\\d{18}|\\d{15}|1[3-9]\\d{9}|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|62\\d{14,17})"
    );

    public static void assertSafePayload(PromptPayload payload) {
        Objects.requireNonNull(payload, "Prompt payload must not be null");
        Objects.requireNonNull(payload.getTableName(), "TableName is required in schema whitelist");
        Objects.requireNonNull(payload.getColumnName(), "ColumnName is required in schema whitelist");

        String combined = String.join(" ",
                payload.getDatabaseName() != null ? payload.getDatabaseName() : "",
                payload.getTableName(),
                payload.getTableComment() != null ? payload.getTableComment() : "",
                payload.getColumnName(),
                payload.getColumnComment() != null ? payload.getColumnComment() : "",
                payload.getDataType() != null ? payload.getDataType() : ""
        );

        if (SUSPECTED_DATA_ROW_PATTERNS.matcher(combined).find()) {
            throw new SecurityException("CRITICAL SAFETY VIOLATION: Prompt payload contains suspected real business data row! Aborting LLM call.");
        }
    }
}
