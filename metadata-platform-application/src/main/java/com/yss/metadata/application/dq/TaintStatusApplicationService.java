package com.yss.metadata.application.dq;

import com.yss.metadata.domain.dq.gateway.TaintStatusGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 资产存疑状态流转应用服务
 *
 * @author ai
 * @since 2026-08-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaintStatusApplicationService {

    private final TaintStatusGateway taintStatusGateway;

    /**
     * 更新资产数据存疑状态
     *
     * @param assetId     资产ID
     * @param taintStatus 存疑状态 (NORMAL / TAINTED)
     * @param reason      原因或备注
     * @param operator    操作人
     */
    public void updateTaintStatus(String assetId, String taintStatus, String reason, String operator) {
        log.info("资产 [{}] 数据存疑状态流转至 [{}], operator: {}", assetId, taintStatus, operator);
        taintStatusGateway.updateTaintStatus(assetId, taintStatus, reason, operator);
    }
}
