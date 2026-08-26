package com.yss.datamiddle.aicontextlayer.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * A2 语义层口径查询网关 Stub（MVP 阶段预留，不阻塞当前切片）。
 *
 * <p>待 A2 语义层服务独立发布后接入真实客户端，当前提供防腐空实现。</p>
 */
@Component
@Slf4j
public class A2GatewayStub {

    /**
     * 根据术语或口径编码查询关联定义。
     *
     * @param termCode 术语或指标编码
     * @return 口径说明列表
     */
    public List<String> queryMetricDefinitions(String termCode) {
        log.debug("A2GatewayStub: 语义层查询口径，当前返回空列表（seam_deferred）: {}", termCode);
        return Collections.emptyList();
    }
}
