-- =============================================================================
-- ACL-SLICE-01 WU-01-02：MCP Server 六表初版 DDL
-- 依据：数据架构 docs/.scratch/ai-context-layer/architecture/ai-context-layer-data-architecture.md §5（物理模型草案）
--       / §12（迁移拆分与回滚）；MCP 工具清单契约 api/mcp-tools-contract-draft.md v1.0-frozen §2（工具白名单）
-- 范围：agent / agent_credential / agent_domain / mcp_session / tool_registry / audit_log 六表初版
-- 安全：agent_credential.credential_ref 为 KMS 密文引用字段（SEC-05 / D3 人工审查点），本 WU 仅建字段不接 KMS；
--       本脚本不含任何明文凭据（无 admin seed、无 password/secret/token/api-key 字面量）。
-- 人工评审：DDL 迁移脚本评审（BAC D1）——责任人/回滚约束见本文件头尾注释。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. agent：Agent 身份主数据（受控配置）
-- -----------------------------------------------------------------------------
CREATE TABLE `agent` (
    `id`         varchar(64)  NOT NULL COMMENT '主键ID（受控配置）',
    `name`       varchar(128) NOT NULL COMMENT 'Agent 名称',
    `enabled`    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '启用状态：1-启用，0-停用',
    `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 身份主数据（受控配置）';

-- -----------------------------------------------------------------------------
-- 2. agent_credential：Agent 凭据（credential_ref 为 KMS 密文引用，不存明文 SEC-05）
-- -----------------------------------------------------------------------------
CREATE TABLE `agent_credential` (
    `id`                 varchar(64)  NOT NULL COMMENT '主键ID',
    `agent_id`           varchar(64)  NOT NULL COMMENT 'Agent 身份标识',
    `credential_version` varchar(32)  NOT NULL COMMENT '凭据版本（吊销/轮换按版本标记）',
    `credential_ref`     varchar(256) NOT NULL COMMENT 'KMS 密文引用（不存明文，SEC-05/D3）',
    `status`             varchar(16)  NOT NULL COMMENT '凭据状态：ACTIVE/REVOKED/ROTATED/EXPIRED',
    `issued_at`          datetime     NOT NULL COMMENT '签发时间',
    `expires_at`         datetime     DEFAULT NULL COMMENT '过期时间',
    `revoked_at`         datetime     DEFAULT NULL COMMENT '吊销时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_credential_agent_version` (`agent_id`, `credential_version`),
    KEY `idx_agent_credential_agent_status` (`agent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 凭据（KMS 密文引用）';

-- -----------------------------------------------------------------------------
-- 3. agent_domain：Agent → 数据域映射（受控配置，与主平台数据域枚举对齐 IC-01）
-- -----------------------------------------------------------------------------
CREATE TABLE `agent_domain` (
    `id`         varchar(64) NOT NULL COMMENT '主键ID',
    `agent_id`   varchar(64) NOT NULL COMMENT 'Agent 身份标识',
    `domain`     varchar(64) NOT NULL COMMENT '数据域（与主平台 domain 枚举对齐，IC-01）',
    `updated_at` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_domain_agent_domain` (`agent_id`, `domain`),
    KEY `idx_agent_domain_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 数据域映射（受控配置）';

-- -----------------------------------------------------------------------------
-- 4. mcp_session：MCP 会话（生命周期 / 吊销强制断开联动）
-- -----------------------------------------------------------------------------
CREATE TABLE `mcp_session` (
    `id`                 varchar(64) NOT NULL COMMENT '主键ID（会话ID）',
    `agent_id`           varchar(64) NOT NULL COMMENT 'Agent 身份标识',
    `credential_version` varchar(32) NOT NULL COMMENT '会话绑定的凭据版本',
    `status`             varchar(16) NOT NULL COMMENT '会话状态：ACTIVE/EXPIRED/TERMINATED',
    `established_at`     datetime    NOT NULL COMMENT '建立时间',
    `last_active_at`     datetime    DEFAULT NULL COMMENT '最近活跃时间（空闲回收依据 REC-05）',
    `expires_at`         datetime    DEFAULT NULL COMMENT '过期时间（会话最大时长）',
    `terminated_at`      datetime    DEFAULT NULL COMMENT '终止时间（吊销强制断开/显式终止）',
    PRIMARY KEY (`id`),
    KEY `idx_mcp_session_agent_status` (`agent_id`, `status`),
    KEY `idx_mcp_session_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 会话';

-- -----------------------------------------------------------------------------
-- 5. tool_registry：只读工具白名单（SEC-09，白名单恰好 5 个只读工具）
--    种子数据冻结（契约第 2 节），变更走冻结后变更流程
-- -----------------------------------------------------------------------------
CREATE TABLE `tool_registry` (
    `tool_name`        varchar(64)  NOT NULL COMMENT '工具名（白名单主键）',
    `version`          varchar(16)  NOT NULL COMMENT '工具版本（契约 v1.0-frozen 对应 1.0.0）',
    `enabled`          tinyint(1)   NOT NULL DEFAULT 1 COMMENT '启用状态：1-启用，0-停用',
    `params_schema_ref` varchar(256) DEFAULT NULL COMMENT '参数 schema 引用（契约工具定义章节）',
    PRIMARY KEY (`tool_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只读工具白名单（SEC-09）';

-- 工具白名单 seed：恰好 5 个只读工具（契约第 2 节；无写/执行/管理工具）
INSERT INTO `tool_registry` (`tool_name`, `version`, `enabled`, `params_schema_ref`) VALUES
('search_assets',       '1.0.0', 1, 'mcp-tools-contract-draft.md#3.1'),
('asset_detail',        '1.0.0', 1, 'mcp-tools-contract-draft.md#3.2'),
('lineage',             '1.0.0', 1, 'mcp-tools-contract-draft.md#3.3'),
('impact_analysis',     '1.0.0', 1, 'mcp-tools-contract-draft.md#3.4'),
('classification_query','1.0.0', 1, 'mcp-tools-contract-draft.md#3.5');

-- -----------------------------------------------------------------------------
-- 6. audit_log：审计留痕（调用即写 SEC-06；初版字段，哈希链 prev_hash/row_hash 由 ACL-SLICE-05 补全）
--    不可变：无修改/删除路径（数据库账号最小权限）；mcp_request_id 每次调用唯一
-- -----------------------------------------------------------------------------
CREATE TABLE `audit_log` (
    `id`                     varchar(64)  NOT NULL COMMENT '主键ID',
    `mcp_request_id`         varchar(64)  NOT NULL COMMENT '每次调用唯一 ID（关联溯源；本表唯一）',
    `session_id`             varchar(64)  NOT NULL COMMENT 'MCP 会话 ID',
    `agent_id`               varchar(64)  NOT NULL COMMENT '凭据主体标识',
    `tool`                   varchar(64)  NOT NULL COMMENT '工具名（鉴权失败/方法拒绝记为连接级/方法级标记）',
    `params_summary`         varchar(1024) DEFAULT NULL COMMENT '参数摘要（不含凭据；不含完整敏感参数值）',
    `result_summary`         varchar(1024) DEFAULT NULL COMMENT '结果摘要（返回条数/耗时/结果码）',
    `result_code`            varchar(32)  NOT NULL COMMENT '结果码：success 或 MCP 错误码',
    `timestamp`              datetime     NOT NULL COMMENT '服务器时间（RFC3339 UTC）',
    `duration_ms`            int          DEFAULT NULL COMMENT '耗时（毫秒）',
    `internal_permission_flag` varchar(32) DEFAULT NULL COMMENT '内部权限判定标记（403/404/越权/域外剔除；仅内部可见，SEC-03）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_audit_log_mcp_request_id` (`mcp_request_id`),
    KEY `idx_audit_log_session_id` (`session_id`),
    KEY `idx_audit_log_agent_timestamp` (`agent_id`, `timestamp`),
    KEY `idx_audit_log_timestamp` (`timestamp` DESC),
    KEY `idx_audit_log_result_code` (`result_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计留痕（调用即写，不可变 SEC-06）';

-- =============================================================================
-- 回滚说明（数据架构 §12：V1 迁移可回滚）：
--   逆序 DROP（先子表后父表，audit_log → mcp_session → agent_domain →
--   tool_registry → agent_credential → agent）：
--
--   DROP TABLE IF EXISTS `audit_log`;
--   DROP TABLE IF EXISTS `mcp_session`;
--   DROP TABLE IF EXISTS `agent_domain`;
--   DROP TABLE IF EXISTS `tool_registry`;
--   DROP TABLE IF EXISTS `agent_credential`;
--   DROP TABLE IF EXISTS `agent`;
--
-- 责任人：D1 人工评审（BAC D1，DDL 迁移脚本评审）；回滚约束：独立 schema/独立部署（数据架构 §10），
-- 无外键引用主平台库表，回滚无级联影响。
-- =============================================================================
