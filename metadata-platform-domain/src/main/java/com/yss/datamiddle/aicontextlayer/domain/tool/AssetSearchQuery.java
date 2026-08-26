package com.yss.datamiddle.aicontextlayer.domain.tool;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 资产检索请求参数（契约 3.1 / SEC-07 资源边界）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_KEYWORD_LENGTH = 256;

    private String keyword;
    private String domain;
    private String classification;
    private String type;
    private String source;
    private String sort;
    @Builder.Default
    private int pageIndex = 1;
    @Builder.Default
    private int pageSize = DEFAULT_PAGE_SIZE;

    public void validate() {
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        if (pageIndex < 1) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }
    }
}
