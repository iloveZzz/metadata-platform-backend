package com.yss.datamiddle.semantic.term.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * 术语分页查询条件（keyword / status / onlyCertified + 分页）。
 */
@Getter
@Builder
public class TermQuery {

    /** 搜索术语名称 / 别名（LIKE 模糊） */
    private final String keyword;

    /** 状态筛选：draft / certified / deprecated；null 表示全部 */
    private final String status;

    /** 仅看已认证（status = certified） */
    private final Boolean onlyCertified;

    /** 页码（1 起） */
    private final int pageIndex;

    /** 页大小（1..200） */
    private final int pageSize;
}
