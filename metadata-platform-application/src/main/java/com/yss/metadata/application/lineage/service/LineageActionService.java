package com.yss.metadata.application.lineage.service;

import com.yss.metadata.client.dto.cmd.LineageManualCmd;
import com.yss.metadata.client.vo.LineageEdgeVO;

/**
 * 人工补录血缘应用服务（WU-03-01）。
 *
 * <p>环检测 CYCLE（409，定位冲突边）+ 图版本 token CONFLICT（409，恢复路径=
 * 重读图谱）+ 重复边幂等返回既有边 + 审计写入（lineage.manual）。</p>
 */
public interface LineageActionService {

    /**
     * 人工补录血缘边（201 语义；事务边界：单聚合事务）。
     *
     * @param cmd      补录命令（from/to/type/confidence/remark/graphVersionToken）
     * @param operator 当前用户（X-User-Id 解析值，缺省 default-user）
     */
    LineageEdgeVO addManualEdge(LineageManualCmd cmd, String operator);
}
