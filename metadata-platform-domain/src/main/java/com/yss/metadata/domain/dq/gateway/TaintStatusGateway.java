package com.yss.metadata.domain.dq.gateway;

/**
 * 资产存疑状态流转网关接口
 *
 * @author ai
 * @since 2026-08-15
 */
public interface TaintStatusGateway {

    /**
     * 更新资产数据存疑状态并写入审计日志
     *
     * @param assetId     资产ID
     * @param taintStatus 存疑状态 (NORMAL / TAINTED)
     * @param reason      原因或备注
     * @param operator    操作人
     */
    void updateTaintStatus(String assetId, String taintStatus, String reason, String operator);
}
