package com.yss.metadata.repository;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.metadata.repository.entity.LineageEdgePO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 血缘边持久化仓库（MyBatis-Plus，BasePlusRepository 接入）。
 */
public interface LineageEdgeRepository extends BasePlusRepository<LineageEdgePO> {

    /**
     * 当前图版本 = 全部边 graph_version 最大值（乐观锁 token；空图返回 null）。
     */
    @Select("SELECT MAX(graph_version) FROM lineage_edge")
    String selectLatestGraphVersion();
}
