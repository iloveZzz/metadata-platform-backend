package com.yss.metadata.infrastructure.dq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yss.metadata.domain.dq.gateway.TaintStatusGateway;
import com.yss.metadata.repository.AssetRepository;
import com.yss.metadata.repository.AuditLogRepository;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AuditLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 资产存疑状态持久化网关实现
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaintStatusGatewayImpl implements TaintStatusGateway {

    private final AssetRepository assetRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public void updateTaintStatus(String assetId, String taintStatus, String reason, String operator) {
        String normalizedStatus = "TAINTED".equalsIgnoreCase(taintStatus) ? "TAINTED" : "NORMAL";

        LambdaUpdateWrapper<AssetPO> updateWrapper = new LambdaUpdateWrapper<AssetPO>()
                .eq(AssetPO::getId, assetId)
                .set(AssetPO::getTaintStatus, normalizedStatus)
                .set(AssetPO::getUpdatedAt, LocalDateTime.now());
        assetRepository.update(null, updateWrapper);

        // 记录审计日志
        try {
            AuditLogPO audit = AuditLogPO.builder()
                    .id(UUID.randomUUID().toString())
                    .action("UPDATE_TAINT_STATUS")
                    .operator(operator != null ? operator : "anonymous")
                    .object(assetId)
                    .result(String.format("状态变更至: %s, 原因: %s", normalizedStatus, reason))
                    .time(LocalDateTime.now())
                    .build();
            auditLogRepository.insert(audit);
        } catch (Exception e) {
            log.warn("存疑状态流转审计记录失败: {}", e.getMessage());
        }
    }

}
