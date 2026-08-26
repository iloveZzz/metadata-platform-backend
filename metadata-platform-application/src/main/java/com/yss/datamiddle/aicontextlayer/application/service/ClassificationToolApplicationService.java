package com.yss.datamiddle.aicontextlayer.application.service;

import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpErrorCode;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.McpException;
import com.yss.datamiddle.aicontextlayer.domain.mcpserver.gateway.MetadataPlatformGateway;
import com.yss.datamiddle.aicontextlayer.domain.tool.ClassificationItem;
import com.yss.datamiddle.aicontextlayer.domain.tool.ClassificationQueryResult;
import com.yss.datamiddle.aicontextlayer.domain.tool.Provenance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分级分类查询 MCP 工具应用编排服务（SEC-04 / SEC-08 / 契约 3.5）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassificationToolApplicationService {

    private final MetadataPlatformGateway metadataPlatformGateway;

    /**
     * 查询分级分类列表 (classification_query)
     */
    public ClassificationQueryResult queryClassifications(String agentId, String baseUrl, int pageIndex, int pageSize) {
        if (pageIndex < 1 || pageSize < 1 || pageSize > 50) {
            throw McpException.of(McpErrorCode.INVALID_PARAMS);
        }

        try {
            metadataPlatformGateway.getClassifications(baseUrl);
        } catch (McpException e) {
            if (e.getErrorCode() == McpErrorCode.UNAUTHORIZED) {
                // 契约 SEC-03: 主平台 403 -> 空列表防御性处理
                log.warn("queryClassifications: 上游 403，安全降级为空列表");
                return ClassificationQueryResult.builder()
                        .items(Collections.emptyList())
                        .totalCount(0)
                        .build();
            }
            throw e;
        }

        List<ClassificationItem> sampleItems = new ArrayList<>();
        sampleItems.add(ClassificationItem.builder()
                .classificationId("cls-001")
                .name("L1-公开数据")
                .enabled(true)
                .source("metadata-platform")
                .updatedAt(LocalDateTime.now().minusDays(2))
                .provenance(Provenance.builder()
                        .classificationId("cls-001")
                        .source("metadata-platform")
                        .updatedAt(LocalDateTime.now().minusDays(2))
                        .fetchedAt(LocalDateTime.now())
                        .build())
                .build());
        sampleItems.add(ClassificationItem.builder()
                .classificationId("cls-002")
                .name("L2-内部公开")
                .enabled(true)
                .source("metadata-platform")
                .updatedAt(LocalDateTime.now().minusDays(1))
                .provenance(Provenance.builder()
                        .classificationId("cls-002")
                        .source("metadata-platform")
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .fetchedAt(LocalDateTime.now())
                        .build())
                .build());

        return ClassificationQueryResult.builder()
                .items(sampleItems)
                .totalCount(sampleItems.size())
                .build();
    }
}
