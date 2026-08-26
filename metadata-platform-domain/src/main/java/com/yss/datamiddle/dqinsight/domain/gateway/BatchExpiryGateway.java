package com.yss.datamiddle.dqinsight.domain.gateway;

import java.time.Instant;

/**
 * 批次过期流转端口（OQ-03 系统自动流转，非用户动作）。
 *
 * <p>低频调度将 valid_until &lt; now 且 status = ingested 的批次置 invalidated；幂等可重跑
 * （第二次执行匹配 0 行），返回受影响行数（数据架构 §7/§8）。</p>
 */
public interface BatchExpiryGateway {

    /**
     * 将超期批次流转为 invalidated。
     *
     * @param now 当前时刻
     * @return 受影响行数（幂等重跑为 0）
     */
    int invalidateExpired(Instant now);
}
