package com.yss.metadata.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 数据源类型统计视图对象（按数据源类型的已创建/已采集统计）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorTypeStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据源类型标识（如 "MySQL", "PolarDB-X"） */
    private String type;

    /** 已创建连接器实例数 */
    private Integer createdCount;

    /** 已采集资产/表数量 */
    private Integer collectedCount;
}
