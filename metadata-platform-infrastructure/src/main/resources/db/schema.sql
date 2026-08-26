-- =====================================================================
-- 数据中台元数据发现与智能治理平台 (Metadata Platform) 全量 Schema 脚本
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `metadata_platform`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `metadata_platform`;

-- 1. Leaf 分布式 ID 发号表
CREATE TABLE IF NOT EXISTS `leaf_alloc` (
    `biz_tag` VARCHAR(128) NOT NULL COMMENT '业务标识/表名',
    `max_id` BIGINT NOT NULL DEFAULT 10000 COMMENT '当前已分配最大ID',
    `step` INT NOT NULL DEFAULT 2000 COMMENT '号段步长',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '业务描述',
    `update_time` VARCHAR(64) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Leaf 号段模式发号配置表';

INSERT IGNORE INTO `leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES
('default', 10000, 2000, '默认全局发号段', '2026-08-16 20:00:00'),
('sec_security_grade', 1000, 500, '安全分级主键发号', '2026-08-16 20:00:00'),
('sec_category_tree', 5000, 1000, '分类目录树节点主键发号', '2026-08-16 20:00:00'),
('sec_data_category', 10000, 2000, '数据分类主键发号', '2026-08-16 20:00:00'),
('sec_sensitive_rule', 10000, 2000, '识别规则主键发号', '2026-08-16 20:00:00'),
('sec_sensitive_record', 100000, 10000, '敏感打标资产记录发号', '2026-08-16 20:00:00'),
('sec_key_secret', 10000, 2000, '安全密钥主键发号', '2026-08-16 20:00:00'),
('sec_masking_rule', 10000, 2000, '脱敏规则主键发号', '2026-08-16 20:00:00'),
('sec_masking_whitelist', 10000, 2000, '脱敏时效白名单主键发号', '2026-08-16 20:00:00'),
('sec_security_audit', 100000, 10000, '安全审计日志主键发号', '2026-08-16 20:00:00');

-- 2. 元数据资产核心表
CREATE TABLE IF NOT EXISTS `data_source` (
    `id` VARCHAR(36) NOT NULL COMMENT '连接器ID（UUID）',
    `name` VARCHAR(128) NOT NULL COMMENT '连接器名称（唯一）',
    `type` VARCHAR(32) NOT NULL COMMENT '连接器类型',
    `host` VARCHAR(255) NOT NULL COMMENT '主机地址',
    `port` INT(11) NOT NULL COMMENT '端口',
    `dialect` VARCHAR(32) NOT NULL COMMENT '方言',
    `username` VARCHAR(128) DEFAULT NULL COMMENT '用户名',
    `cred_ref` VARCHAR(512) DEFAULT NULL COMMENT '凭据加密引用',
    `auto_classify` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否自动识别分类',
    `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '状态',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_source_name` (`name`),
    KEY `idx_data_source_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源（连接器）表';

CREATE TABLE IF NOT EXISTS `collector_task` (
    `id` VARCHAR(36) NOT NULL COMMENT '采集任务ID（UUID）',
    `name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `connector_id` VARCHAR(36) NOT NULL COMMENT '目标数据源ID',
    `schedule` VARCHAR(128) NOT NULL COMMENT '调度',
    `mode` VARCHAR(32) NOT NULL COMMENT '采集模式',
    `strategy` VARCHAR(32) NOT NULL DEFAULT 'ignore' COMMENT '覆盖策略',
    `auto_classify` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否自动识别分类',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
    `fail_reason` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    `last_run_at` DATETIME DEFAULT NULL COMMENT '最近执行开始时间',
    `owner` VARCHAR(64) DEFAULT '1397905662202719' COMMENT '负责人',
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '采集任务描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '生效状态',
    `datasource_type` VARCHAR(64) DEFAULT NULL COMMENT '数据源类型',
    `source_system` VARCHAR(64) DEFAULT NULL COMMENT '来源系统',
    `scope_type` VARCHAR(32) DEFAULT 'all' COMMENT '采集范围',
    `selected_databases` TEXT DEFAULT NULL COMMENT '指定 Database 列表',
    `retry_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开启失败重试',
    `retry_count` INT DEFAULT 1 COMMENT '重试次数',
    `retry_interval` INT DEFAULT 5 COMMENT '重试间隔（分钟）',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collector_task_source_schedule` (`connector_id`, `schedule`),
    KEY `idx_collector_task_connector` (`connector_id`),
    KEY `idx_collector_task_status` (`status`),
    KEY `idx_collector_task_owner` (`owner`),
    KEY `idx_collector_task_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务表';

CREATE TABLE IF NOT EXISTS `asset` (
    `id` VARCHAR(36) NOT NULL COMMENT '资产ID（UUID）',
    `source_id` VARCHAR(36) NOT NULL COMMENT '来源数据源ID',
    `name` VARCHAR(255) NOT NULL COMMENT '资产名称',
    `type` VARCHAR(32) NOT NULL COMMENT '资产类型：table/column/view',
    `domain` VARCHAR(64) DEFAULT NULL COMMENT '数据域',
    `owner` VARCHAR(128) DEFAULT NULL COMMENT '负责人',
    `classification` VARCHAR(64) DEFAULT NULL COMMENT '分级分类',
    `database_name` VARCHAR(100) DEFAULT NULL COMMENT '所属数据库/Schema名称',
    `source_system` VARCHAR(100) DEFAULT NULL COMMENT '来源业务系统编码或名称',
    `collector_task_id` VARCHAR(36) DEFAULT NULL COMMENT '最后一次采集的任务ID',
    `is_excluded` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已剔除/软删除（0:正常, 1:已剔除）',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
    `taint_status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '数据存疑状态',
    `version` VARCHAR(64) DEFAULT NULL COMMENT '最新版本号',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述/注释',
    `row_count` BIGINT DEFAULT NULL COMMENT '表行数',
    `storage_size` VARCHAR(64) DEFAULT NULL COMMENT '存储量',
    `updated_at` DATETIME DEFAULT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_asset_source` (`source_id`),
    KEY `idx_asset_source_db` (`source_id`, `database_name`),
    KEY `idx_asset_source_system` (`source_system`),
    KEY `idx_asset_is_excluded` (`is_excluded`),
    KEY `idx_asset_domain_classification` (`domain`, `classification`),
    KEY `idx_asset_updated_at` (`updated_at`),
    KEY `idx_asset_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='元数据资产表';

CREATE TABLE IF NOT EXISTS `asset_column` (
    `id` VARCHAR(36) NOT NULL COMMENT '列ID（UUID）',
    `asset_id` VARCHAR(36) NOT NULL COMMENT '所属资产ID',
    `name` VARCHAR(128) NOT NULL COMMENT '列名',
    `type` VARCHAR(64) DEFAULT NULL COMMENT '列类型',
    `comment` VARCHAR(512) DEFAULT NULL COMMENT '列注释',
    `pk` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主键',
    `ordinal_position` INT(11) DEFAULT NULL COMMENT '物理序号/列定义顺序',
    `classification` VARCHAR(64) DEFAULT NULL COMMENT '分级分类',
    PRIMARY KEY (`id`),
    KEY `idx_asset_column_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产列（明细）表';

CREATE TABLE IF NOT EXISTS `asset_version` (
    `id` VARCHAR(36) NOT NULL COMMENT '版本ID（UUID）',
    `asset_id` VARCHAR(36) NOT NULL COMMENT '资产ID',
    `version` INT(11) NOT NULL COMMENT '版本号',
    `schema_diff` TEXT COMMENT 'schema 变更内容',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_version` (`asset_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产版本表';

CREATE TABLE IF NOT EXISTS `asset_favorite` (
    `asset_id` VARCHAR(36) NOT NULL COMMENT '资产ID（UUID）',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT NULL COMMENT '收藏时间',
    PRIMARY KEY (`asset_id`, `user_id`),
    KEY `idx_asset_favorite_user` (`user_id`, `asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

CREATE TABLE IF NOT EXISTS `asset_tag` (
    `asset_id` VARCHAR(36) NOT NULL COMMENT '资产ID（UUID）',
    `tag` VARCHAR(64) NOT NULL COMMENT '标签',
    PRIMARY KEY (`asset_id`, `tag`),
    KEY `idx_asset_tag` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产标签表';

CREATE TABLE IF NOT EXISTS `lineage_edge` (
    `id` VARCHAR(36) NOT NULL COMMENT '边ID（UUID）',
    `from_asset` VARCHAR(36) NOT NULL COMMENT '上游资产ID',
    `to_asset` VARCHAR(36) NOT NULL COMMENT '下游资产ID',
    `from_column_id` VARCHAR(36) DEFAULT NULL COMMENT '上游字段ID',
    `to_column_id` VARCHAR(36) DEFAULT NULL COMMENT '下游字段ID',
    `transform_expr` VARCHAR(1024) DEFAULT NULL COMMENT '字段转换表达式',
    `expr_type` VARCHAR(32) NOT NULL DEFAULT 'DIRECT' COMMENT '表达式类型：DIRECT/COMPUTED/AGGREGATE/MANUAL',
    `type` VARCHAR(32) NOT NULL COMMENT '血缘类型：sql/job/manual',
    `confidence` VARCHAR(32) NOT NULL COMMENT '置信度',
    `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `graph_version` VARCHAR(64) DEFAULT NULL COMMENT '图版本',
    PRIMARY KEY (`id`),
    KEY `idx_lineage_from` (`from_asset`),
    KEY `idx_lineage_to` (`to_asset`),
    KEY `idx_lineage_col_from` (`from_column_id`, `to_column_id`),
    KEY `idx_lineage_col_to` (`to_column_id`, `from_column_id`),
    KEY `idx_lineage_asset_col_pair` (`from_asset`, `from_column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='血缘边表';

CREATE TABLE IF NOT EXISTS `export_task` (
    `id` VARCHAR(36) NOT NULL COMMENT '任务ID（UUID）',
    `asset_id` VARCHAR(36) DEFAULT NULL COMMENT '导出源资产',
    `format` VARCHAR(16) NOT NULL COMMENT '导出格式：csv/json/datahub',
    `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态',
    `file_ref` VARCHAR(512) DEFAULT NULL COMMENT '生成文件引用',
    `operator` VARCHAR(64) DEFAULT NULL COMMENT '触发人',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_export_asset_status` (`asset_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出异步任务表';

CREATE TABLE IF NOT EXISTS `classification` (
    `id` VARCHAR(36) NOT NULL COMMENT '分类ID（UUID）',
    `asset_id` VARCHAR(36) DEFAULT NULL COMMENT '资产ID',
    `column_id` VARCHAR(36) DEFAULT NULL COMMENT '列ID',
    `name` VARCHAR(128) NOT NULL COMMENT '分类名',
    `level` VARCHAR(32) DEFAULT NULL COMMENT '敏感等级',
    `source` VARCHAR(32) DEFAULT NULL COMMENT '来源：auto/manual',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
    PRIMARY KEY (`id`),
    KEY `idx_classification_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分级分类结果表';

CREATE TABLE IF NOT EXISTS `class_rule` (
    `id` VARCHAR(36) NOT NULL COMMENT '规则ID（UUID）',
    `name` VARCHAR(128) NOT NULL COMMENT '规则名',
    `type` VARCHAR(32) NOT NULL COMMENT '规则类型',
    `pattern` VARCHAR(1024) DEFAULT NULL COMMENT '匹配模式',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (`id`),
    KEY `idx_class_rule_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类规则表';

CREATE TABLE IF NOT EXISTS `data_domain` (
    `id` VARCHAR(36) NOT NULL COMMENT '数据域ID（UUID）',
    `name` VARCHAR(64) NOT NULL COMMENT '数据域名（唯一）',
    `owner` VARCHAR(128) DEFAULT NULL COMMENT '负责人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_domain_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据域表';

CREATE TABLE IF NOT EXISTS `role` (
    `id` VARCHAR(36) NOT NULL COMMENT '角色ID（UUID）',
    `name` VARCHAR(64) NOT NULL COMMENT '角色名（唯一）',
    `scope` VARCHAR(64) DEFAULT NULL COMMENT '角色范围',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `role_domain` (
    `role_id` VARCHAR(36) NOT NULL COMMENT '角色ID',
    `domain_id` VARCHAR(36) NOT NULL COMMENT '数据域ID',
    PRIMARY KEY (`role_id`, `domain_id`),
    KEY `idx_role_domain_domain` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-数据域关联表';

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` VARCHAR(36) NOT NULL COMMENT '审计ID（UUID）',
    `operator` VARCHAR(128) DEFAULT NULL COMMENT '操作者',
    `action` VARCHAR(64) NOT NULL COMMENT '动作',
    `object` VARCHAR(128) DEFAULT NULL COMMENT '操作对象',
    `result` VARCHAR(16) DEFAULT NULL COMMENT '结果',
    `time` DATETIME DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_log_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS `propagate_task` (
    `id` VARCHAR(36) NOT NULL COMMENT '任务ID（UUID）',
    `classification_id` VARCHAR(36) NOT NULL COMMENT '触发源分类',
    `version` VARCHAR(64) DEFAULT NULL COMMENT '传播版本',
    `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态',
    `coverage` VARCHAR(512) DEFAULT NULL COMMENT '覆盖范围',
    `operator` VARCHAR(64) DEFAULT NULL COMMENT '触发人',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_propagate_class_status` (`classification_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类传播异步任务表';

CREATE TABLE IF NOT EXISTS `integration_config` (
    `id` VARCHAR(36) NOT NULL COMMENT '单例配置行（固定 id=1）',
    `gravitino_endpoint` VARCHAR(512) DEFAULT NULL COMMENT 'Gravitino 端点地址',
    `gravitino_auth_ref` VARCHAR(512) DEFAULT NULL COMMENT 'Gravitino 认证',
    `gravitino_enabled` TINYINT(1) DEFAULT 0 COMMENT 'Gravitino 上游是否启用',
    `gravitino_last_test` VARCHAR(512) DEFAULT NULL COMMENT '最近测试连接结果',
    `datahub_endpoint` VARCHAR(512) DEFAULT NULL COMMENT 'DataHub 导出目标地址',
    `datahub_auth_ref` VARCHAR(512) DEFAULT NULL COMMENT 'DataHub 认证',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集成配置表';

CREATE TABLE IF NOT EXISTS `openlineage_event` (
    `id` VARCHAR(36) NOT NULL COMMENT '事件记录ID（UUID）',
    `event_type` VARCHAR(16) NOT NULL COMMENT 'START/COMPLETE/FAIL/ABORT',
    `event_time` DATETIME DEFAULT NULL COMMENT '事件时间',
    `run_id` VARCHAR(64) DEFAULT NULL COMMENT 'Run 标识',
    `job_namespace` VARCHAR(256) DEFAULT NULL COMMENT '作业命名空间',
    `job_name` VARCHAR(256) DEFAULT NULL COMMENT '作业名',
    `parse_status` VARCHAR(16) NOT NULL DEFAULT 'received' COMMENT '状态',
    `received_at` DATETIME DEFAULT NULL COMMENT '接收时间',
    PRIMARY KEY (`id`),
    KEY `idx_openlineage_received` (`received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenLineage 事件接收记录表';

CREATE TABLE IF NOT EXISTS `ai_ask_log` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
    `user_id` VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    `query_text` VARCHAR(512) NOT NULL COMMENT '自然语言查询',
    `matched_asset_ids` VARCHAR(512) DEFAULT NULL COMMENT '匹配资产ID列表',
    `confidence_score` VARCHAR(32) DEFAULT NULL COMMENT '置信度评分',
    `model_name` VARCHAR(64) DEFAULT NULL COMMENT '使用模型或降级标识',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_ask_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 智能找数审计表';

CREATE TABLE IF NOT EXISTS `dq_root_cause_record` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键ID',
    `target_asset_id` VARCHAR(36) NOT NULL COMMENT '目标故障资产ID',
    `root_asset_id` VARCHAR(36) NOT NULL COMMENT '根因故障资产ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '失败规则名称',
    `actual_metric` VARCHAR(128) DEFAULT NULL COMMENT '实际指标值',
    `threshold` VARCHAR(128) DEFAULT NULL COMMENT '质量阈值',
    `confidence` VARCHAR(32) DEFAULT NULL COMMENT '置信度',
    `fault_time` VARCHAR(32) DEFAULT NULL COMMENT '故障发生时间',
    `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    `created_at` DATETIME DEFAULT NULL COMMENT '分析时间',
    PRIMARY KEY (`id`),
    KEY `idx_rc_target` (`target_asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量故障溯源分析历史表';

-- 3. 数据质量 (DQ) 表
CREATE TABLE IF NOT EXISTS `dq_batch` (
    `id`              BIGINT       NOT NULL,
    `batch_no`        VARCHAR(64)  NOT NULL,
    `source_tool`     VARCHAR(32)  NOT NULL,
    `format_type`     VARCHAR(16)  NOT NULL,
    `channel_id`      VARCHAR(64)  NULL,
    `status`          VARCHAR(32)  NOT NULL,
    `linkage_status`  VARCHAR(16)  NOT NULL DEFAULT 'none',
    `received_at`     DATETIME     NOT NULL,
    `execution_time`  DATETIME     NULL,
    `row_count`       INT          NOT NULL DEFAULT 0,
    `error_category`  VARCHAR(16)  NULL,
    `error_message`   VARCHAR(2048) NULL,
    `valid_until`     DATETIME     NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_dq_batch_source_tool_batch_no` UNIQUE (`source_tool`, `batch_no`),
    KEY `idx_dq_batch_channel_id` (`channel_id`),
    KEY `idx_dq_batch_status` (`status`),
    KEY `idx_dq_batch_received_at` (`received_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量采集批次表';

CREATE TABLE IF NOT EXISTS `dq_rule_result` (
    `id`              BIGINT        NOT NULL,
    `batch_id`        BIGINT        NOT NULL,
    `asset_id`        VARCHAR(128)  NULL,
    `field_name`      VARCHAR(255)  NULL,
    `rule_name`       VARCHAR(255)  NOT NULL,
    `rule_type`       VARCHAR(32)   NOT NULL,
    `status`          VARCHAR(16)   NOT NULL,
    `failure_reason`  VARCHAR(2048) NULL,
    `execution_time`  DATETIME      NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dq_rule_result_batch_id` (`batch_id`),
    KEY `idx_dq_rule_result_asset` (`asset_id`, `field_name`),
    KEY `idx_dq_rule_result_asset_status` (`asset_id`, `field_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则执行结果表';

CREATE TABLE IF NOT EXISTS `dq_asset_linkage` (
    `id`                BIGINT        NOT NULL,
    `batch_id`          BIGINT        NOT NULL,
    `source_asset_id`   VARCHAR(128)  NOT NULL,
    `resolved_asset_id` VARCHAR(128)  NULL,
    `asset_name`        VARCHAR(255)  NULL,
    `domain`            VARCHAR(255)  NULL,
    `asset_type`        VARCHAR(32)   NULL,
    `match_mode`        VARCHAR(16)   NULL,
    `state`             VARCHAR(16)   NOT NULL,
    `created_at`        DATETIME      NOT NULL,
    `mapped_at`         DATETIME      NULL,
    `mapped_by`         VARCHAR(64)   NULL,
    `note`              VARCHAR(1024) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_dq_asset_linkage_batch_asset` UNIQUE (`batch_id`, `source_asset_id`),
    KEY `idx_dq_asset_linkage_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产关联映射表';

CREATE TABLE IF NOT EXISTS `dq_audit_log` (
    `id`          BIGINT        NOT NULL,
    `event_time`  DATETIME      NOT NULL,
    `operator`    VARCHAR(64)   NULL,
    `action`      VARCHAR(32)   NOT NULL,
    `object`      VARCHAR(255)  NULL,
    `result`      VARCHAR(16)   NOT NULL,
    `detail`      VARCHAR(2048) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dq_audit_log_event_time` (`event_time`),
    KEY `idx_dq_audit_log_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量审计日志表';

CREATE TABLE IF NOT EXISTS `dq_health_score` (
    `id`              BIGINT        NOT NULL,
    `asset_id`        VARCHAR(128)  NOT NULL,
    `field_name`      VARCHAR(255)  NULL,
    `asset_name`      VARCHAR(255)  NULL,
    `domain`          VARCHAR(255)  NULL,
    `asset_type`      VARCHAR(32)   NULL,
    `score`           INT           NULL,
    `band`            VARCHAR(8)    NULL,
    `state`           VARCHAR(16)   NOT NULL,
    `rule_version`    VARCHAR(16)   NOT NULL,
    `batch_id`        BIGINT        NOT NULL,
    `computed_at`     DATETIME      NOT NULL,
    `pass_rate`       VARCHAR(8)    NULL,
    `last_result_at`  DATETIME      NULL,
    `valid_until`     DATETIME      NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_dq_health_score_asset_field` UNIQUE (`asset_id`, `field_name`),
    KEY `idx_dq_health_score_domain_band` (`domain`, `band`),
    KEY `idx_dq_health_score_domain_state` (`domain`, `state`),
    KEY `idx_dq_health_score_band` (`band`),
    KEY `idx_dq_health_score_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产健康分表';

CREATE TABLE IF NOT EXISTS `dq_rule_detail` (
    `id`              BIGINT        NOT NULL,
    `batch_id`        BIGINT        NOT NULL,
    `asset_id`        VARCHAR(128)  NOT NULL,
    `field_name`      VARCHAR(255)  NULL,
    `rule_name`       VARCHAR(255)  NOT NULL,
    `rule_type`       VARCHAR(32)   NOT NULL,
    `status`          VARCHAR(16)   NOT NULL,
    `weight`          DECIMAL(4,2)  NOT NULL,
    `failure_reason`  VARCHAR(2048) NULL,
    `execution_time`  DATETIME      NULL,
    `rule_version`    VARCHAR(16)   NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_dq_rule_detail_batch_asset_field_rule` UNIQUE (`batch_id`, `asset_id`, `field_name`, `rule_name`),
    KEY `idx_dq_rule_detail_asset` (`asset_id`),
    KEY `idx_dq_rule_detail_batch_asset_field` (`batch_id`, `asset_id`, `field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则细项得分表';

CREATE TABLE IF NOT EXISTS `dq_channel` (
    `id`              BIGINT         NOT NULL,
    `name`            VARCHAR(255)   NOT NULL,
    `type`            VARCHAR(32)    NOT NULL,
    `schedule`        VARCHAR(64)    NULL,
    `format_type`     VARCHAR(32)    NOT NULL,
    `auth_token_enc`  TEXT           NULL,
    `auth_configured` TINYINT(1)     NOT NULL DEFAULT 0,
    `domain`          VARCHAR(255)   NULL,
    `state`           VARCHAR(16)    NOT NULL,
    `last_pull_at`    DATETIME       NULL,
    `last_error`      VARCHAR(1024)  NULL,
    `error_category`  VARCHAR(16)    NULL,
    `deleted_at`      DATETIME       NULL,
    `created_by`      VARCHAR(64)    NULL,
    `created_at`      DATETIME       NOT NULL,
    `updated_at`      DATETIME       NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_dq_channel_name` UNIQUE (`name`),
    KEY `idx_dq_channel_state` (`state`),
    KEY `idx_dq_channel_type_state` (`type`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量接入通道表';

-- 4. 智能安全分类分级与指标对齐治理引擎 (Smart Governance)
CREATE TABLE IF NOT EXISTS `sg_compliance_template` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '模板主键 ID',
    `template_code` VARCHAR(64) NOT NULL COMMENT '模板唯一编码',
    `template_name` VARCHAR(128) NOT NULL COMMENT '模板中文名称',
    `standard_authority` VARCHAR(128) NULL COMMENT '制定机构/标准号',
    `description` VARCHAR(512) NULL COMMENT '模板详细说明',
    `default_auto_approval` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认开启高置信自动打标',
    `default_threshold` DECIMAL(4,2) NOT NULL DEFAULT 0.95 COMMENT '默认自动打标置信度阈值',
    `is_system_built_in` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为系统内置模板',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' COMMENT '更新人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `uk_sg_tpl_code` UNIQUE (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业合规模板表';

CREATE TABLE IF NOT EXISTS `sg_classification_rule` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '规则主键 ID',
    `template_id` VARCHAR(64) NOT NULL COMMENT '所属模板 ID',
    `sensitive_type` VARCHAR(64) NOT NULL COMMENT '敏感数据类型',
    `sensitive_name` VARCHAR(128) NOT NULL COMMENT '敏感类型中文名',
    `security_level` VARCHAR(16) NOT NULL COMMENT '安全级别',
    `clause_ref` VARCHAR(256) NULL COMMENT '法规条款引用',
    `regex_pattern` VARCHAR(512) NULL COMMENT 'L1 正则预筛表达式',
    `dictionary_words` TEXT NULL COMMENT 'L2 敏感词典',
    `semantic_prompt` TEXT NULL COMMENT 'L3 大模型少样本 Prompt 指引',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `priority` INT NOT NULL DEFAULT 100 COMMENT '规则优先级',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' COMMENT '创建人',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'SYSTEM' COMMENT '更新人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_sg_rule_tpl_lvl` (`template_id`, `security_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类分级识别规则配置表';

CREATE TABLE IF NOT EXISTS `sg_candidate` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '候选记录主键 ID',
    `template_id` VARCHAR(64) NOT NULL COMMENT '参照合规模板 ID',
    `rule_id` VARCHAR(64) NULL COMMENT '命中规则 ID',
    `data_source` VARCHAR(64) NOT NULL COMMENT '数据源名称',
    `database_name` VARCHAR(128) NOT NULL COMMENT '数据库名',
    `table_name` VARCHAR(128) NOT NULL COMMENT '数据表名',
    `column_name` VARCHAR(128) NOT NULL COMMENT '字段名',
    `column_comment` VARCHAR(512) NULL COMMENT '字段注释',
    `data_type` VARCHAR(64) NOT NULL COMMENT '字段物理类型',
    `sensitive_type` VARCHAR(64) NOT NULL COMMENT '识别出的敏感类型',
    `recommended_level` VARCHAR(16) NOT NULL COMMENT '推荐安全级别',
    `clause_ref` VARCHAR(256) NULL COMMENT '命中法规条款',
    `reasoning` VARCHAR(512) NOT NULL COMMENT '判定依据理由',
    `confidence` DECIMAL(4,2) NOT NULL COMMENT '置信度',
    `funnel_layer` VARCHAR(32) NOT NULL COMMENT '产出层级',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `actual_level` VARCHAR(16) NULL COMMENT '最终生效的安全级别',
    `operator` VARCHAR(64) NULL COMMENT '审核处理人',
    `review_comment` VARCHAR(512) NULL COMMENT '审核备注说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '扫描发现时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '审核处理时间',
    INDEX `idx_sg_cdd_status_lvl` (`status`, `recommended_level`),
    INDEX `idx_sg_cdd_col` (`database_name`, `table_name`, `column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全打标识别候选池表';

CREATE TABLE IF NOT EXISTS `sg_security_audit_log` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '审计主键 ID',
    `candidate_id` VARCHAR(64) NULL COMMENT '关联打标候选 ID',
    `data_source` VARCHAR(64) NOT NULL COMMENT '数据源名称',
    `database_name` VARCHAR(128) NOT NULL COMMENT '数据库名',
    `table_name` VARCHAR(128) NOT NULL COMMENT '数据表名',
    `column_name` VARCHAR(128) NOT NULL COMMENT '字段名',
    `previous_level` VARCHAR(16) NULL COMMENT '操作前安全级别',
    `new_level` VARCHAR(16) NOT NULL COMMENT '操作后安全级别',
    `action_type` VARCHAR(32) NOT NULL COMMENT '操作类型',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `reason` VARCHAR(512) NOT NULL COMMENT '变更依据说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间戳',
    INDEX `idx_sg_adt_col` (`database_name`, `table_name`, `column_name`),
    INDEX `idx_sg_adt_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打标全链路审计留痕表';

CREATE TABLE IF NOT EXISTS `sg_metric_conflict` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '冲突事件主键 ID',
    `conflict_code` VARCHAR(64) NOT NULL COMMENT '冲突事件业务编号',
    `indicator_a_id` VARCHAR(64) NOT NULL COMMENT '指标 A 主键 ID',
    `indicator_a_name` VARCHAR(128) NOT NULL COMMENT '指标 A 中文名称',
    `indicator_a_code` VARCHAR(64) NOT NULL COMMENT '指标 A 英文编码',
    `indicator_a_domain` VARCHAR(64) NOT NULL COMMENT '指标 A 所属业务域',
    `indicator_b_id` VARCHAR(64) NOT NULL COMMENT '指标 B 主键 ID',
    `indicator_b_name` VARCHAR(128) NOT NULL COMMENT '指标 B 中文名称',
    `indicator_b_code` VARCHAR(64) NOT NULL COMMENT '指标 B 英文编码',
    `indicator_b_domain` VARCHAR(64) NOT NULL COMMENT '指标 B 所属业务域',
    `conflict_type` VARCHAR(32) NOT NULL COMMENT '冲突类型',
    `similarity_score` DECIMAL(4,2) NOT NULL COMMENT '综合相似度',
    `formula_a` TEXT NULL COMMENT '指标 A 计算公式',
    `formula_b` TEXT NULL COMMENT '指标 B 计算公式',
    `ast_diff_summary` TEXT NULL COMMENT 'AST 差异 JSON 摘要',
    `status` VARCHAR(32) NOT NULL DEFAULT 'UNRESOLVED' COMMENT '状态',
    `canonical_id` VARCHAR(64) NULL COMMENT '权威主指标 ID',
    `resolution_comment` VARCHAR(512) NULL COMMENT '治理处理备注',
    `operator` VARCHAR(64) NULL COMMENT '治理专员',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发现时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '治理时间',
    CONSTRAINT `uk_sg_mcf_code` UNIQUE (`conflict_code`),
    INDEX `idx_sg_mcf_status` (`status`),
    INDEX `idx_sg_mcf_pair` (`indicator_a_id`, `indicator_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标语义与 AST 冲突事件表';

CREATE TABLE IF NOT EXISTS `sg_metric_reconciliation_log` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '记录主键 ID',
    `conflict_id` VARCHAR(64) NOT NULL COMMENT '关联冲突事件 ID',
    `canonical_id` VARCHAR(64) NOT NULL COMMENT '权威主指标 ID',
    `alias_id` VARCHAR(64) NOT NULL COMMENT '转化为别名的冗余指标 ID',
    `migrated_asset_count` INT NOT NULL DEFAULT 0 COMMENT '平滑迁移的关联资产数量',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `reconcile_strategy` VARCHAR(64) NOT NULL COMMENT '对齐策略',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间戳',
    INDEX `idx_sg_mrc_cfl` (`conflict_id`),
    INDEX `idx_sg_mrc_canon` (`canonical_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标对齐与归并审计表';

-- 5. 数据安全与动态脱敏中心 (Data Security)
CREATE TABLE IF NOT EXISTS `sec_security_grade` (
    `id` BIGINT NOT NULL COMMENT '分级ID',
    `grade_name` VARCHAR(128) NOT NULL COMMENT '分级名称',
    `grade_code` VARCHAR(64) NOT NULL COMMENT '分级编码',
    `sensitivity_score` INT NOT NULL DEFAULT 1 COMMENT '敏感度权重',
    `color_tag` VARCHAR(32) NOT NULL DEFAULT 'blue' COMMENT '色彩标签',
    `description` VARCHAR(2048) DEFAULT NULL COMMENT '分级描述',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sec_grade_name` (`grade_name`),
    UNIQUE KEY `uk_sec_grade_code` (`grade_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分级表';

CREATE TABLE IF NOT EXISTS `sec_category_tree` (
    `id` BIGINT NOT NULL COMMENT '目录树节点ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID',
    `node_name` VARCHAR(128) NOT NULL COMMENT '节点名称',
    `node_path` VARCHAR(1024) NOT NULL DEFAULT '/' COMMENT '节点全路径',
    `depth_level` INT NOT NULL DEFAULT 1 COMMENT '目录层级',
    `visibility` VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围',
    `admins` VARCHAR(512) DEFAULT NULL COMMENT '管理员列表(JSON)',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_tree_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类目录树表';

CREATE TABLE IF NOT EXISTS `sec_data_category` (
    `id` BIGINT NOT NULL COMMENT '分类ID',
    `category_name` VARCHAR(512) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(128) NOT NULL COMMENT '分类编码',
    `tree_node_id` BIGINT NOT NULL COMMENT '所属目录树节点ID',
    `security_grade_id` BIGINT NOT NULL COMMENT '关联安全分级ID',
    `priority` INT NOT NULL DEFAULT 3 COMMENT '识别优先级',
    `scan_dimension_config` TEXT DEFAULT NULL COMMENT '6维高级扫描特征配置(JSON)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `disable_policy` VARCHAR(32) NOT NULL DEFAULT 'RETAIN_TAGS' COMMENT '停用策略',
    `description` VARCHAR(2048) DEFAULT NULL COMMENT '分类描述',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_cat_tree` (`tree_node_id`),
    KEY `idx_sec_cat_grade` (`security_grade_id`),
    KEY `idx_sec_cat_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据分类表';

CREATE TABLE IF NOT EXISTS `sec_sensitive_rule` (
    `id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '特征名称',
    `rule_type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '特征类型(BUILTIN/CUSTOM)',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '特征使用场景描述',
    `priority` INT NOT NULL DEFAULT 50 COMMENT '执行优先级',
    `owner` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '负责人',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `category_scope_mode` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '分类圈选模式',
    `category_scope_ids` TEXT DEFAULT NULL COMMENT '分类范围ID列表(JSON)',
    `scan_scope_type` VARCHAR(32) NOT NULL DEFAULT 'DATASOURCE' COMMENT '扫描源类型',
    `scan_scope_config` TEXT DEFAULT NULL COMMENT '扫描源过滤条件(JSON)',
    `feature_config` TEXT DEFAULT NULL COMMENT '多模态特征配置(JSON)',
    `tagged_fields_count` INT NOT NULL DEFAULT 0 COMMENT '已打标字段数',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_rule_status` (`status`),
    KEY `idx_sec_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感识别特征规则表';

-- 预置内置识别特征数据
INSERT IGNORE INTO `sec_sensitive_rule` (`id`, `rule_name`, `rule_type`, `description`, `priority`, `owner`, `status`, `category_scope_mode`, `scan_scope_type`, `feature_config`, `tagged_fields_count`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(1001, '性别', 'BUILTIN', '用于识别男女、性别相关字段', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"COLUMN_NAME","matchMode":"REGEX_CASE_INSENSITIVE","value":".*(gender|sex|性别).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1002, 'IP(v4)地址', 'BUILTIN', '识别标准IPv4互联网协议地址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1003, 'URL地址', 'BUILTIN', '识别标准HTTP/HTTPS网址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_CASE_INSENSITIVE","value":"^https?://.*","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1004, '经营许可证', 'BUILTIN', '企业经营与业务资质许可证号识别', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"OR","rules":[{"scanType":"COLUMN_NAME","matchMode":"CONTAINS","value":"license"},{"scanType":"COLUMN_COMMENT","matchMode":"CONTAINS","value":"经营许可证"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1005, '车辆种类', 'BUILTIN', '机动车及营运车辆类型标识', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"COLUMN_COMMENT","matchMode":"CONTAINS","value":"车辆种类"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1006, '交易金额', 'BUILTIN', '识别金融交易、结算及账户金额相关数值字段', 30, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"DATA_TYPE","matchMode":"IN_LIST","dataTypes":["decimal","bigint","int"]},{"scanType":"COLUMN_NAME","matchMode":"REGEX_CASE_INSENSITIVE","value":".*(amount|balance|fee|amt|money|金额|余额).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1007, '中文姓名', 'BUILTIN', '识别2~4位常见中文汉字姓名', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"COLUMN_NAME","matchMode":"REGEX_CASE_INSENSITIVE","value":".*(user_name|cust_name|real_name|name|姓名|客户名).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1008, '城市', 'BUILTIN', '国内及国际城市与区域名称', 25, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"COLUMN_NAME","matchMode":"REGEX_CASE_INSENSITIVE","value":".*(city|城市).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1009, '证件类型', 'BUILTIN', '身份证、护照、港澳通行证等证件类别编码', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"COLUMN_NAME","matchMode":"REGEX_CASE_INSENSITIVE","value":".*(cert_type|id_type|证件类型).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1010, '身份证号(中国台湾)', 'BUILTIN', '中国台湾地区10位身分证字号格式识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^[A-Z][12]\\d{8}$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1011, '居民身份证(中国大陆)', 'BUILTIN', '中国大陆18位第二代居民身份证号码识别', 5, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1012, '移动电话', 'BUILTIN', '中国大陆11位手机号码识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^1[3-9]\\d{9}$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1013, '电子邮箱', 'BUILTIN', '标准Email电子邮箱格式', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_CASE_INSENSITIVE","value":"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1014, '银行卡号', 'BUILTIN', '国内16~19位银联借记卡与信用卡号', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^[1-9]\\d{15,18}$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1015, '统一社会信用代码', 'BUILTIN', '中国大陆18位法人和其他组织统一社会信用代码', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"logic":"AND","rules":[{"scanType":"CONTENT","matchMode":"REGEX_EXACT","value":"^[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$","threshold":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10');

CREATE TABLE IF NOT EXISTS `sec_sensitive_record` (
    `id` BIGINT NOT NULL COMMENT '打标记录ID',
    `datasource_id` VARCHAR(64) NOT NULL COMMENT '数据源ID',
    `datasource_name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
    `schema_name` VARCHAR(128) DEFAULT NULL COMMENT 'Schema名称',
    `table_name` VARCHAR(256) NOT NULL COMMENT '表名',
    `field_name` VARCHAR(128) NOT NULL COMMENT '字段名',
    `field_comment` VARCHAR(512) DEFAULT NULL COMMENT '字段注释',
    `category_id` BIGINT NOT NULL COMMENT '识别分类ID',
    `category_name` VARCHAR(512) NOT NULL COMMENT '识别分类名称',
    `security_grade_id` BIGINT NOT NULL COMMENT '关联安全分级ID',
    `security_grade_name` VARCHAR(128) NOT NULL COMMENT '关联安全分级名称',
    `sensitivity_score` INT NOT NULL DEFAULT 1 COMMENT '敏感度权重',
    `matched_rule_id` BIGINT DEFAULT NULL COMMENT '命中规则ID',
    `matched_rule_name` VARCHAR(64) DEFAULT NULL COMMENT '命中规则名称',
    `source_type` VARCHAR(32) NOT NULL DEFAULT 'RULE' COMMENT '来源类型',
    `is_locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定校准状态',
    `lock_user` VARCHAR(64) DEFAULT NULL COMMENT '锁定人',
    `lock_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
    `sample_data` VARCHAR(1024) DEFAULT NULL COMMENT '敏感数据样例',
    `sample_preview` VARCHAR(1024) DEFAULT NULL COMMENT '敏感数据样例脱敏预览',
    `confidence_score` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度',
    `status` VARCHAR(32) NOT NULL DEFAULT 'UNCONFIRMED' COMMENT '状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '识别时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sec_field_record` (`datasource_id`, `table_name`, `field_name`),
    KEY `idx_sec_rec_ds_table` (`datasource_id`, `table_name`),
    KEY `idx_sec_rec_grade` (`security_grade_id`),
    KEY `idx_sec_rec_cat` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感识别打标记录表';

CREATE TABLE IF NOT EXISTS `sec_key_secret` (
    `id` BIGINT NOT NULL COMMENT '密钥ID',
    `key_name` VARCHAR(128) NOT NULL COMMENT '密钥名称',
    `key_type` VARCHAR(32) NOT NULL COMMENT '密钥类型',
    `algorithm` VARCHAR(32) NOT NULL COMMENT '算法',
    `encrypted_key_value` TEXT NOT NULL COMMENT '主密钥加密存储值',
    `public_key_value` TEXT DEFAULT NULL COMMENT '非对称公钥明文',
    `owner` VARCHAR(64) NOT NULL COMMENT '负责人',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `referenced_rules_count` INT NOT NULL DEFAULT 0 COMMENT '引用脱敏规则数',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sec_key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全密钥管理表';

CREATE TABLE IF NOT EXISTS `sec_masking_rule` (
    `id` BIGINT NOT NULL COMMENT '脱敏规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `category_id` BIGINT NOT NULL COMMENT '绑定数据分类ID',
    `algorithm_type` VARCHAR(32) NOT NULL COMMENT '算法类型',
    `algorithm_config` TEXT DEFAULT NULL COMMENT '掩码参数配置(JSON)',
    `algorithm_params` TEXT DEFAULT NULL COMMENT '掩码参数(JSON)',
    `key_id` BIGINT DEFAULT NULL COMMENT '关联密钥ID',
    `scope_type` VARCHAR(32) NOT NULL DEFAULT 'GLOBAL' COMMENT '生效范围',
    `scope_target` TEXT DEFAULT NULL COMMENT '范围目标明细(JSON)',
    `apply_scene` VARCHAR(32) NOT NULL DEFAULT 'QUERY_DISPLAY' COMMENT '应用场景',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_mask_cat` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脱敏规则配置表';

CREATE TABLE IF NOT EXISTS `sec_masking_whitelist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '白名单ID',
    `whitelist_name` VARCHAR(128) NOT NULL DEFAULT '时效免脱敏白名单' COMMENT '白名单名称',
    `grantee_type` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '授权主体类型',
    `grantee_id` VARCHAR(64) NOT NULL COMMENT '主体ID',
    `category_id` BIGINT NOT NULL COMMENT '关联数据分类ID',
    `rule_id` BIGINT NOT NULL COMMENT '关联脱敏规则ID',
    `start_time` DATETIME NOT NULL COMMENT '生效开始时间',
    `end_time` DATETIME NOT NULL COMMENT '生效截止时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `reason` VARCHAR(1024) NOT NULL COMMENT '申请原因',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_wl_grantee` (`grantee_type`, `grantee_id`),
    KEY `idx_sec_wl_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态脱敏时效白名单表';

CREATE TABLE IF NOT EXISTS `sec_security_audit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
    `action_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `client_ip` VARCHAR(64) NOT NULL DEFAULT '127.0.0.1' COMMENT '客户端IP',
    `target_resource` VARCHAR(256) NOT NULL COMMENT '操作对象资源',
    `action_detail` TEXT DEFAULT NULL COMMENT '详细操作快照(JSON)',
    `risk_level` VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '风险级别',
    `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_audit_action` (`action_type`),
    KEY `idx_sec_audit_time` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全审计日志表';

-- 6. 智能找数与资产打标 (Smart Discovery)
CREATE TABLE IF NOT EXISTS `sd_tag_category` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '分类主键 ID',
    `category_name` VARCHAR(128) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '分类编码',
    `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父分类 ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签分类表';

CREATE TABLE IF NOT EXISTS `sd_tag_definition` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '标签主键 ID',
    `tag_name` VARCHAR(128) NOT NULL COMMENT '标签名称',
    `tag_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '标签编码',
    `category_code` VARCHAR(64) NOT NULL COMMENT '所属分类编码',
    `color_token` VARCHAR(32) DEFAULT 'blue' COMMENT '前端颜色 Token',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '标签描述',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_by` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签定义表';

CREATE TABLE IF NOT EXISTS `sd_tag_rule` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '规则主键 ID',
    `tag_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '关联标签 ID',
    `regex_pattern` VARCHAR(512) DEFAULT NULL COMMENT 'Layer 1 正则表达式',
    `bound_term_ids` JSON DEFAULT NULL COMMENT 'Layer 2 关联中文术语 ID 列表',
    `bound_term_names` JSON DEFAULT NULL COMMENT 'Layer 2 关联中文术语名称列表',
    `few_shot_prompt` TEXT DEFAULT NULL COMMENT 'Layer 3 大模型 Few-Shot 提示词模版',
    `scope_filter` VARCHAR(256) DEFAULT NULL COMMENT '圈选作用范围',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签三层打标规则表';

CREATE TABLE IF NOT EXISTS `sd_tag_candidate` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '候选建议主键 ID',
    `table_name` VARCHAR(128) NOT NULL COMMENT '目标数据表名',
    `column_name` VARCHAR(128) NOT NULL COMMENT '目标字段名',
    `column_comment` VARCHAR(256) DEFAULT NULL COMMENT '目标字段注释',
    `current_tag` VARCHAR(128) DEFAULT NULL COMMENT '当前已有标签',
    `recommended_tag_id` VARCHAR(64) NOT NULL COMMENT '推荐标签 ID',
    `recommended_tag_name` VARCHAR(128) NOT NULL COMMENT '推荐标签名称',
    `tag_category` VARCHAR(64) NOT NULL COMMENT '标签类型',
    `source` VARCHAR(32) NOT NULL COMMENT '推导来源',
    `confidence` DECIMAL(5,4) NOT NULL COMMENT '推导置信度',
    `inference_reason` VARCHAR(1024) DEFAULT NULL COMMENT '推导依据与解释',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `batch_id` VARCHAR(64) DEFAULT NULL COMMENT '处理批次 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_sd_table_column` (`table_name`, `column_name`),
    INDEX `idx_sd_status_conf` (`status`, `confidence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能打标候选池表';

CREATE TABLE IF NOT EXISTS `sd_tag_audit_log` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '审计日志 ID',
    `batch_id` VARCHAR(64) NOT NULL COMMENT '操作批次 ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '操作类型',
    `action_name` VARCHAR(64) NOT NULL COMMENT '操作名称',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `field_count` INT NOT NULL DEFAULT 1 COMMENT '影响字段数',
    `snapshot_diff` JSON DEFAULT NULL COMMENT '变更快照差异',
    `status` VARCHAR(32) NOT NULL DEFAULT 'APPLIED' COMMENT '批次状态',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    INDEX `idx_sd_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打标操作审计与回滚日志表';

CREATE TABLE IF NOT EXISTS `sd_llm_gateway_config` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '配置主键 ID',
    `provider` VARCHAR(64) NOT NULL DEFAULT 'deepseek' COMMENT '模型提供商',
    `base_url` VARCHAR(256) NOT NULL COMMENT 'BaseURL',
    `api_key_encrypted` VARCHAR(512) NOT NULL COMMENT 'API Key',
    `model_name` VARCHAR(128) NOT NULL DEFAULT 'deepseek-chat' COMMENT '模型名称',
    `auto_apply_threshold` INT NOT NULL DEFAULT 90 COMMENT '高置信自动生效阈值',
    `timeout_ms` INT NOT NULL DEFAULT 5000 COMMENT '超时毫秒数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型网关接入配置表';

-- 7. 语义层与术语库 (Semantic Layer)
CREATE TABLE IF NOT EXISTS `term` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花 id）',
    `name`           VARCHAR(200) NOT NULL COMMENT '术语名称（全局唯一）',
    `definition`     TEXT         NOT NULL COMMENT '术语定义',
    `description`    VARCHAR(1000) DEFAULT NULL COMMENT '描述',
    `owner`          VARCHAR(100) NOT NULL COMMENT '负责人',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT '状态',
    `certified_by`   VARCHAR(100)  DEFAULT NULL COMMENT '认证人',
    `certified_at`   DATETIME      DEFAULT NULL COMMENT '认证时间',
    `deprecated_by`  VARCHAR(100)  DEFAULT NULL COMMENT '弃用人',
    `deprecated_at`  DATETIME      DEFAULT NULL COMMENT '弃用时间',
    `synonym_set_id` BIGINT        DEFAULT NULL COMMENT '关联同义词组 id',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_by`     VARCHAR(100) NOT NULL COMMENT '创建人',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_name` (`name`),
    UNIQUE KEY `uk_term_synonym_set_id` (`synonym_set_id`),
    KEY `idx_term_status` (`status`),
    KEY `idx_term_updated_at` (`updated_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='术语库聚合根表';

CREATE TABLE IF NOT EXISTS `term_alias` (
    `id`      BIGINT       NOT NULL COMMENT '主键（雪花 id）',
    `term_id` BIGINT       NOT NULL COMMENT '术语 id',
    `alias`   VARCHAR(200) NOT NULL COMMENT '别名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_alias` (`term_id`, `alias`),
    KEY `idx_term_alias_term_id` (`term_id`),
    KEY `idx_term_alias_alias` (`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='术语别名表';

-- 8. AI Context Layer (MCP Agent)
CREATE TABLE IF NOT EXISTS `agent` (
    `id`         VARCHAR(64)  NOT NULL COMMENT '主键ID（受控配置）',
    `name`       VARCHAR(128) NOT NULL COMMENT 'Agent 名称',
    `enabled`    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_agent_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 身份主数据表';

CREATE TABLE IF NOT EXISTS `agent_credential` (
    `id`                 VARCHAR(64)  NOT NULL COMMENT '主键ID',
    `agent_id`           VARCHAR(64)  NOT NULL COMMENT 'Agent 身份标识',
    `credential_version` VARCHAR(32)  NOT NULL COMMENT '凭据版本',
    `credential_ref`     VARCHAR(256) NOT NULL COMMENT 'KMS 密文引用',
    `status`             VARCHAR(16)  NOT NULL COMMENT '凭据状态',
    `issued_at`          DATETIME     NOT NULL COMMENT '签发时间',
    `expires_at`         DATETIME     DEFAULT NULL COMMENT '过期时间',
    `revoked_at`         DATETIME     DEFAULT NULL COMMENT '吊销时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_credential_agent_version` (`agent_id`, `credential_version`),
    KEY `idx_agent_credential_agent_status` (`agent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 凭据表';

CREATE TABLE IF NOT EXISTS `agent_domain` (
    `id`         VARCHAR(64) NOT NULL COMMENT '主键ID',
    `agent_id`   VARCHAR(64) NOT NULL COMMENT 'Agent 身份标识',
    `domain`     VARCHAR(64) NOT NULL COMMENT '数据域',
    `updated_at` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_domain_agent_domain` (`agent_id`, `domain`),
    KEY `idx_agent_domain_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 数据域映射表';

CREATE TABLE IF NOT EXISTS `mcp_session` (
    `id`                 VARCHAR(64) NOT NULL COMMENT '主键ID',
    `agent_id`           VARCHAR(64) NOT NULL COMMENT 'Agent 身份标识',
    `credential_version` VARCHAR(32) NOT NULL COMMENT '会话绑定的凭据版本',
    `status`             VARCHAR(16) NOT NULL COMMENT '会话状态',
    `established_at`     DATETIME    NOT NULL COMMENT '建立时间',
    `last_active_at`     DATETIME    DEFAULT NULL COMMENT '最近活跃时间',
    `expires_at`         DATETIME    DEFAULT NULL COMMENT '过期时间',
    `terminated_at`      DATETIME    DEFAULT NULL COMMENT '终止时间',
    PRIMARY KEY (`id`),
    KEY `idx_mcp_session_agent_status` (`agent_id`, `status`),
    KEY `idx_mcp_session_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 会话管理表';

CREATE TABLE IF NOT EXISTS `tool_registry` (
    `tool_name`         VARCHAR(64)  NOT NULL COMMENT '工具名',
    `version`           VARCHAR(16)  NOT NULL COMMENT '工具版本',
    `enabled`           TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态',
    `params_schema_ref` VARCHAR(256) DEFAULT NULL COMMENT '参数 schema 引用',
    PRIMARY KEY (`tool_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只读工具白名单表';

INSERT IGNORE INTO `tool_registry` (`tool_name`, `version`, `enabled`, `params_schema_ref`) VALUES
('search_assets',       '1.0.0', 1, 'mcp-tools-contract-draft.md#3.1'),
('asset_detail',        '1.0.0', 1, 'mcp-tools-contract-draft.md#3.2'),
('lineage',             '1.0.0', 1, 'mcp-tools-contract-draft.md#3.3'),
('impact_analysis',     '1.0.0', 1, 'mcp-tools-contract-draft.md#3.4'),
('classification_query','1.0.0', 1, 'mcp-tools-contract-draft.md#3.5');

-- -----------------------------------------------------
-- db-scheduler 分布式任务调度核心表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `scheduled_tasks` (
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务大类标识（如 METADATA_COLLECTOR）',
    `task_instance` VARCHAR(100) NOT NULL COMMENT '任务实例唯一标识（对应 collector_task.id）',
    `task_data` LONGBLOB DEFAULT NULL COMMENT '任务上下文序列化数据',
    `execution_time` TIMESTAMP(6) NOT NULL COMMENT '下次触发时间戳（微秒精度）',
    `picked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否被节点抢占调度',
    `picked_by` VARCHAR(50) DEFAULT NULL COMMENT '抢占节点标识（IP:PID/主机名）',
    `last_success` TIMESTAMP(6) NULL DEFAULT NULL COMMENT '上次成功时间',
    `last_failure` TIMESTAMP(6) NULL DEFAULT NULL COMMENT '上次失败时间',
    `consecutive_failures` INT DEFAULT 0 COMMENT '连续失败计数',
    `last_heartbeat` TIMESTAMP(6) NULL DEFAULT NULL COMMENT '抢占节点心跳时间',
    `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`task_name`, `task_instance`),
    INDEX `idx_scheduled_tasks_execution_time` (`execution_time`),
    INDEX `idx_scheduled_tasks_last_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='db-scheduler 分布式任务调度队列表';
