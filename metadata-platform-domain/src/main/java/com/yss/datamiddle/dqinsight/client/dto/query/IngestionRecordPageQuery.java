package com.yss.datamiddle.dqinsight.client.dto.query;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import lombok.Getter;
import lombok.Setter;

/**
 * 接入记录分页查询（GET /api/dq/results：sourceTool / channelId / status / linkageStatus 筛选 + 分页）。
 */
@Getter
@Setter
public class IngestionRecordPageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 来源工具筛选 */
    private SourceTool sourceTool;

    /** 接入通道筛选 */
    private String channelId;

    /** 接入状态筛选 */
    private IngestionStatus status;

    /** 关联状态筛选 */
    private LinkageState linkageStatus;
}
