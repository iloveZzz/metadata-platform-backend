package com.yss.metadata.domain.integration.spi;

import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.domain.integration.model.GravitinoEndpoint;

/**
 * Gravitino 防腐层端口（集成域；Domain 定义，Infrastructure 实现）。
 *
 * <p>消费 Gravitino OpenLineage 事件 + REST API 作为采集上游（ADR-0003）；
 * 隔离外部模型（Gravitino 原始模型不泄露为本产品契约）。真实客户端 seam-deferred
 * （PoC 未认证，同切片 03 GaussDB 方言先例）：当前实现返回分类失败提示，
 * 不伪装接入；测试连接失败分类（network/credential/dialect）。</p>
 */
public interface GravitinoGateway {

    /**
     * 测试 Gravitino 连接（防腐层：仅暴露端点/认证；外部模型隔离）。
     */
    ConnectTestResult testConnection(GravitinoEndpoint endpoint);
}
