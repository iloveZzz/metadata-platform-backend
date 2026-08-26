-- =====================================================================
-- 智能数据安全分类分级与指标对齐治理引擎（SG-SLICE-01 ~ 05）数据表
-- 依据：docs/.scratch/smart-governance/architecture/smart-governance-data-architecture.md
-- =====================================================================

-- 1. 行业合规模板表
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

-- 2. 分类分级识别规则配置表
CREATE TABLE IF NOT EXISTS `sg_classification_rule` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '规则主键 ID',
    `template_id` VARCHAR(64) NOT NULL COMMENT '所属模板 ID',
    `sensitive_type` VARCHAR(64) NOT NULL COMMENT '敏感数据类型',
    `sensitive_name` VARCHAR(128) NOT NULL COMMENT '敏感类型中文名',
    `security_level` VARCHAR(16) NOT NULL COMMENT '安全级别 (L1~L5)',
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

-- 3. 安全打标识别候选池表
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

-- 4. 打标全链路审计留痕表
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

-- 5. 指标语义与 AST 冲突事件表
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

-- 6. 指标对齐与归并审计表
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
