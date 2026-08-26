package com.yss.datamiddle.aicontextlayer.domain.mcpserver;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent 数据域映射（受控配置，数据架构 §5）。
 *
 * <p>domain 与主平台数据域枚举为逻辑对齐（无外键，IC-01，走冻结 API 核验），
 * 不一致按冻结后变更流程处理，不得修改冻结 YAML。</p>
 */
@Getter
@Setter
public class AgentDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** Agent 身份标识 */
    private String agentId;

    /** 数据域（与主平台 domain 枚举对齐，IC-01） */
    private String domain;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
