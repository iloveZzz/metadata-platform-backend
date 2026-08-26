package com.yss.metadata.domain.ai.gateway;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能找数审计网关端口
 *
 * @author ai
 * @since 2026-08-15
 */
public interface AskMetadataLogGateway {

    /**
     * 记录智能找数日志
     *
     * @param id 主键ID
     * @param userId 操作用户ID
     * @param queryText 查询文本
     * @param matchedAssetIds 匹配的资产ID列表
     * @param confidenceScore 综合置信度
     * @param modelName 模型或降级标识
     * @param createdAt 创建时间
     */
    void logAsk(String id, String userId, String queryText, List<String> matchedAssetIds, String confidenceScore, String modelName, LocalDateTime createdAt);
}
