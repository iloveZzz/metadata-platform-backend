package com.yss.datamiddle.dqinsight.domain.gateway;

import com.yss.datamiddle.dqinsight.domain.model.FetchResult;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;

/**
 * 通道拉取取数端口（外部 DQ 工具原始内容获取）。
 *
 * <p>契约未定义拉取 URL（切片 04 人工审查点 / P1 与部署联调定稿）；MVP 拉取地址由部署配置
 * dq.pull.base-url 提供，路径 {baseUrl}/pull/{channelId}。失败分类 format / auth / network。</p>
 */
public interface ChannelFetchPort {

    /**
     * 取回通道原始结果内容（成功含内容与内容类型；失败分类 + 脱敏信息）。
     */
    FetchResult fetch(IngestionChannel channel);
}
