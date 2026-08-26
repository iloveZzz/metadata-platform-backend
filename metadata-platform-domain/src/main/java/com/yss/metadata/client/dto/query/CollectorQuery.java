package com.yss.metadata.client.dto.query;

import com.yss.cloud.dto.QueryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 采集任务查询对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorQuery extends QueryDTO {

    private static final long serialVersionUID = 1L;

    /** 任务名称或数据来源关键字 */
    private String keyword;

    /** 负责人（工号/用户ID） */
    private String owner;

    /** 生效状态（true/false） */
    private Boolean enabled;

    /** 数据源类型（MySQL, Oracle, ClickHouse 等） */
    private String datasourceType;

    /** 采集模式（incremental/full） */
    private String mode;

    /** 任务状态（pending/running/success/failed/cancelled） */
    private String status;
}
