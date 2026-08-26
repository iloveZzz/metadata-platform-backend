package com.yss.datamiddle.dqinsight.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 待关联队列分页查询（GET /api/dq/asset-linkage/pending；page / size，空队列以空分页表达）。
 *
 * <p>切片 05 追加可见数据域过滤：待关联资产归属 = 来源通道域（batch.channel_id →
 * dq_channel.domain，结果来源口径）；可见域为空 = 不限制；受限用户对域不可判定
 * （无通道 / 通道无域）的记录按域外隐藏（C24 不泄露，人工审查点见 05 证据）。</p>
 */
@Getter
@Setter
public class PendingLinkagePageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 当前用户可见数据域（DataDomainGuard 横切；null / 空 = 不做域限制） */
    private List<String> visibleDomains;
}
