package com.yss.datamiddle.aicontextlayer.repository.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.cloud.dto.page.PageQuery;

/**
 * 分页参数工具：{@link PageQuery} → MyBatis-Plus {@link Page}。
 *
 * <p>WU-01-02 生成的 GatewayImpl 统一经此构建分页对象，配合 MyBatis-Plus
 * {@code PaginationInnerInterceptor} 完成 selectPage 分页（yss-mybatis 规范：
 * 沿用 PageQuery 传递链路，不在 Repository 内临时发明分页参数）。</p>
 */
public final class PageUtil {

    private PageUtil() {
    }

    /**
     * 将 YSS 分页查询参数转换为 MyBatis-Plus 分页对象。
     *
     * @param query 分页查询参数（pageIndex / pageSize）
     * @param <T>   分页记录类型
     * @return MyBatis-Plus Page
     */
    public static <T> Page<T> page(PageQuery query) {
        com.github.pagehelper.PageHelper.clearPage();
        return new Page<>(query.getPageIndex(), query.getPageSize());
    }
}
