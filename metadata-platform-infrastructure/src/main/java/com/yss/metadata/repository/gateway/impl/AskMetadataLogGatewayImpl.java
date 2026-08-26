package com.yss.metadata.repository.gateway.impl;

import com.yss.metadata.domain.ai.gateway.AskMetadataLogGateway;
import com.yss.metadata.repository.AiAskLogRepository;
import com.yss.metadata.repository.entity.AiAskLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能找数审计网关实现
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AskMetadataLogGatewayImpl implements AskMetadataLogGateway {

    private final AiAskLogRepository aiAskLogRepository;

    @Override
    @Async
    public void logAsk(String id, String userId, String queryText, List<String> matchedAssetIds, String confidenceScore, String modelName, LocalDateTime createdAt) {
        try {
            String joinedIds = (matchedAssetIds != null && !matchedAssetIds.isEmpty()) ? String.join(",", matchedAssetIds) : "";
            AiAskLogPO po = AiAskLogPO.builder()
                    .id(id)
                    .userId(userId)
                    .queryText(queryText)
                    .matchedAssetIds(joinedIds)
                    .confidenceScore(confidenceScore)
                    .modelName(modelName)
                    .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                    .build();
            aiAskLogRepository.insert(po);
        } catch (Exception e) {
            log.error("保存 AI 智能找数审计日志失败", e);
        }
    }
}
