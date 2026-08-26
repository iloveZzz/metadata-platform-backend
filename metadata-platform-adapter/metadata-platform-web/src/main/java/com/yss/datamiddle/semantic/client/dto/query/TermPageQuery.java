package com.yss.datamiddle.semantic.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 术语列表查询（冻结契约 GET /api/semantic/terms 筛选参数）。
 */
@Getter
@Setter
public class TermPageQuery extends PageQuery {

    /** 搜索术语名称 / 别名 */
    private String keyword;

    /** 状态筛选：draft / certified / deprecated */
    private String status;

    /** 仅看已认证 */
    private Boolean onlyCertified;
}
