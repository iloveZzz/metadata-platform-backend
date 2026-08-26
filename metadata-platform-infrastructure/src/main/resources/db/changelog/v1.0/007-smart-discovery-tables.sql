-- =====================================================================
-- 智能找数与资产自动打标深化（SD-01 ~ 05）数据表
-- 依据：docs/.scratch/smart-discovery/architecture/smart-discovery-data-architecture.md
-- =====================================================================

-- 1. 标签分类表
CREATE TABLE IF NOT EXISTS `sd_tag_category` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '分类主键 ID',
    `category_name` VARCHAR(128) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '分类编码 (DOMAIN/SECURITY/LIFECYCLE)',
    `parent_id` VARCHAR(64) DEFAULT NULL COMMENT '父分类 ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签分类表';

-- 2. 标签定义表
CREATE TABLE IF NOT EXISTS `sd_tag_definition` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '标签主键 ID',
    `tag_name` VARCHAR(128) NOT NULL COMMENT '标签名称',
    `tag_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '标签编码 (如 SEC_L4)',
    `category_code` VARCHAR(64) NOT NULL COMMENT '所属分类编码',
    `color_token` VARCHAR(32) DEFAULT 'blue' COMMENT '前端颜色 Token',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '标签描述',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    `created_by` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签定义表';

-- 3. 标签三层打标规则表
CREATE TABLE IF NOT EXISTS `sd_tag_rule` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '规则主键 ID',
    `tag_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '关联标签 ID',
    `regex_pattern` VARCHAR(512) DEFAULT NULL COMMENT 'Layer 1 正则表达式',
    `bound_term_ids` JSON DEFAULT NULL COMMENT 'Layer 2 关联中文术语 ID 列表',
    `bound_term_names` JSON DEFAULT NULL COMMENT 'Layer 2 关联中文术语名称列表',
    `few_shot_prompt` TEXT DEFAULT NULL COMMENT 'Layer 3 大模型 Few-Shot 提示词模版',
    `scope_filter` VARCHAR(256) DEFAULT NULL COMMENT '圈选作用范围 (如 dwd_% 表)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签三层打标规则表';

-- 4. 智能打标候选池表
CREATE TABLE IF NOT EXISTS `sd_tag_candidate` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '候选建议主键 ID',
    `table_name` VARCHAR(128) NOT NULL COMMENT '目标数据表名',
    `column_name` VARCHAR(128) NOT NULL COMMENT '目标字段名',
    `column_comment` VARCHAR(256) DEFAULT NULL COMMENT '目标字段注释',
    `current_tag` VARCHAR(128) DEFAULT NULL COMMENT '当前已有标签',
    `recommended_tag_id` VARCHAR(64) NOT NULL COMMENT '推荐标签 ID',
    `recommended_tag_name` VARCHAR(128) NOT NULL COMMENT '推荐标签名称',
    `tag_category` VARCHAR(64) NOT NULL COMMENT '标签类型 (DOMAIN/SECURITY)',
    `source` VARCHAR(32) NOT NULL COMMENT '推导来源 (L1_RULE/L2_DICT/L3_LLM)',
    `confidence` DECIMAL(5,4) NOT NULL COMMENT '推导置信度 (0.0000 ~ 1.0000)',
    `inference_reason` VARCHAR(1024) DEFAULT NULL COMMENT '推导依据与解释',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/AUTO_APPLIED/MANUAL_APPROVED/REJECTED/ROLLED_BACK',
    `batch_id` VARCHAR(64) DEFAULT NULL COMMENT '处理批次 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_sd_table_column` (`table_name`, `column_name`),
    INDEX `idx_sd_status_conf` (`status`, `confidence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能打标候选池表';

-- 5. 打标操作审计与回滚日志表
CREATE TABLE IF NOT EXISTS `sd_tag_audit_log` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '审计日志 ID',
    `batch_id` VARCHAR(64) NOT NULL COMMENT '操作批次 ID',
    `action_type` VARCHAR(32) NOT NULL COMMENT '操作类型 (AUTO_APPLY/MANUAL_APPROVE/REJECT/ROLLBACK)',
    `action_name` VARCHAR(64) NOT NULL COMMENT '操作名称',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人或系统账号',
    `field_count` INT NOT NULL DEFAULT 1 COMMENT '影响字段数',
    `snapshot_diff` JSON DEFAULT NULL COMMENT '变更快照差异 (支持逆向恢复)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'APPLIED' COMMENT '批次状态 (APPLIED/ROLLED_BACK)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    INDEX `idx_sd_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打标操作审计与回滚日志表';

-- 6. 大模型网关接入配置表
CREATE TABLE IF NOT EXISTS `sd_llm_gateway_config` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '配置主键 ID',
    `provider` VARCHAR(64) NOT NULL DEFAULT 'deepseek' COMMENT '模型提供商 (deepseek/qwen/local)',
    `base_url` VARCHAR(256) NOT NULL COMMENT 'OpenAI 兼容 BaseURL',
    `api_key_encrypted` VARCHAR(512) NOT NULL COMMENT '加密存储的 API Key',
    `model_name` VARCHAR(128) NOT NULL DEFAULT 'deepseek-chat' COMMENT '模型名称',
    `auto_apply_threshold` INT NOT NULL DEFAULT 90 COMMENT '高置信自动生效阈值 (80~98)',
    `timeout_ms` INT NOT NULL DEFAULT 5000 COMMENT '超时熔断毫秒数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型网关接入配置表';
