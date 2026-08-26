package com.yss.metadata.application.lineage.service;

import com.yss.metadata.client.dto.cmd.ColumnLineageManualCmd;
import com.yss.metadata.client.vo.ColumnLineageGraphVO;
import com.yss.metadata.client.vo.LineageEdgeVO;

/**
 * 字段级血缘应用服务接口。
 */
public interface ColumnLineageAppService {

    /**
     * 查询资产字段级血缘图谱。
     *
     * @param assetId   中心资产 ID
     * @param columnId  可选过滤字段 ID
     * @param depth     查询深度
     * @param direction 查询方向 (UPSTREAM / DOWNSTREAM / BOTH)
     * @return 字段血缘图谱 VO
     */
    ColumnLineageGraphVO getColumnLineageGraph(String assetId, String columnId, Integer depth, String direction);

    /**
     * 手工补录字段级血缘。
     *
     * @param cmd      补录命令
     * @param operator 当前操作人
     * @return 生成的血缘边 VO
     */
    LineageEdgeVO addManualColumnEdge(ColumnLineageManualCmd cmd, String operator);

    /**
     * 删除字段级血缘边。
     *
     * @param edgeId   边 ID
     * @param operator 当前操作人
     */
    void deleteColumnEdge(String edgeId, String operator);
}
