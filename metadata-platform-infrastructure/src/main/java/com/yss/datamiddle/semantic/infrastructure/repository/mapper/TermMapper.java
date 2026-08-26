package com.yss.datamiddle.semantic.infrastructure.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.datamiddle.semantic.infrastructure.repository.po.TermPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 术语 Mapper（term 表，MyBatis-Plus BaseMapper CRUD + 自定义筛选分页）。
 */
public interface TermMapper extends BasePlusRepository<TermPO> {

    /**
     * 分页查询（keyword 匹配名称 / 别名，status / onlyCertified 筛选，updated_at DESC）。
     */
    List<TermPO> selectTermPage(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("onlyCertified") Boolean onlyCertified,
                                @Param("offset") int offset,
                                @Param("size") int size);

    /**
     * 分页总条数（与 selectTermPage 同条件）。
     */
    long countTermPage(@Param("keyword") String keyword,
                       @Param("status") String status,
                       @Param("onlyCertified") Boolean onlyCertified);
}
