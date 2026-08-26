-- =====================================================================
-- 数据安全中心（SEC-SLICE-01 ~ 06）数据库初始化脚本
-- 依据：docs/.scratch/data-security/architecture/data-security-data-architecture.md
-- =====================================================================

-- 1. 数据分级表
CREATE TABLE IF NOT EXISTS `sec_security_grade` (
    `id` BIGINT NOT NULL COMMENT '分级ID',
    `grade_name` VARCHAR(128) NOT NULL COMMENT '分级名称',
    `grade_code` VARCHAR(64) NOT NULL COMMENT '分级编码',
    `sensitivity_score` INT NOT NULL DEFAULT 1 COMMENT '敏感度权重(1~100)',
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

-- 2. 分类目录树表 (<= 10 级)
CREATE TABLE IF NOT EXISTS `sec_category_tree` (
    `id` BIGINT NOT NULL COMMENT '目录树节点ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父节点ID',
    `node_name` VARCHAR(128) NOT NULL COMMENT '节点名称',
    `node_path` VARCHAR(1024) NOT NULL DEFAULT '/' COMMENT '节点全路径',
    `depth_level` INT NOT NULL DEFAULT 1 COMMENT '目录层级',
    `visibility` VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围(PUBLIC/ADMIN_ONLY)',
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

-- 3. 数据分类表
CREATE TABLE IF NOT EXISTS `sec_data_category` (
    `id` BIGINT NOT NULL COMMENT '分类ID',
    `category_name` VARCHAR(512) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(128) NOT NULL COMMENT '分类编码',
    `tree_node_id` BIGINT NOT NULL COMMENT '所属目录树节点ID',
    `security_grade_id` BIGINT NOT NULL COMMENT '关联安全分级ID',
    `priority` INT NOT NULL DEFAULT 3 COMMENT '识别优先级(1~5, 1最高)',
    `scan_dimension_config` TEXT DEFAULT NULL COMMENT '6维高级扫描特征配置(JSON)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
    `disable_policy` VARCHAR(32) NOT NULL DEFAULT 'RETAIN_TAGS' COMMENT '停用策略(RETAIN_TAGS/DELETE_TAGS)',
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

-- 4. 识别特征/敏感识别规则表
CREATE TABLE IF NOT EXISTS `sec_sensitive_rule` (
    `id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '特征名称',
    `rule_type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '特征类型(BUILTIN/CUSTOM)',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '特征使用场景描述',
    `priority` INT NOT NULL DEFAULT 50 COMMENT '执行优先级(1~100)',
    `owner` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '负责人',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
    `category_scope_mode` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '分类圈选模式(ALL/TREE_NODE/SPECIFIC)',
    `category_scope_ids` TEXT DEFAULT NULL COMMENT '分类范围ID列表(JSON)',
    `scan_scope_type` VARCHAR(32) NOT NULL DEFAULT 'DATASOURCE' COMMENT '扫描源类型(COMPUTE_ENGINE/DATASOURCE)',
    `scan_scope_config` TEXT DEFAULT NULL COMMENT '扫描源过滤条件(JSON)',
    `feature_config` TEXT DEFAULT NULL COMMENT '多模态识别特征条件配置(JSON)',
    `tagged_fields_count` INT NOT NULL DEFAULT 0 COMMENT '已打标字段数',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_rule_status` (`status`),
    KEY `idx_sec_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感数据识别特征规则表';

-- 5. 敏感识别记录表 (字段级打标与人机校准)
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
    `source_type` VARCHAR(32) NOT NULL DEFAULT 'RULE' COMMENT '来源类型(RULE/REALTIME/MANUAL)',
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

-- 8. 安全密钥管理表
CREATE TABLE IF NOT EXISTS `sec_key_secret` (
    `id` BIGINT NOT NULL COMMENT '密钥ID',
    `key_name` VARCHAR(128) NOT NULL COMMENT '密钥名称',
    `key_type` VARCHAR(32) NOT NULL COMMENT '密钥类型(HASH_SALT/SYMMETRIC/ASYMMETRIC)',
    `algorithm` VARCHAR(32) NOT NULL COMMENT '算法(AES_128/AES_256/SM4/DES/RSA_2048/FPE_FF1/MD5_SALT/SHA256_SALT)',
    `encrypted_key_value` TEXT NOT NULL COMMENT '主密钥加密存储值',
    `public_key_value` TEXT DEFAULT NULL COMMENT '非对称公钥明文',
    `owner` VARCHAR(64) NOT NULL COMMENT '负责人',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    `referenced_rules_count` INT NOT NULL DEFAULT 0 COMMENT '引用脱敏规则数',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sec_key_name` (`key_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全密钥管理表';

-- 9. 脱敏规则配置表 (动态/静态)
CREATE TABLE IF NOT EXISTS `sec_masking_rule` (
    `id` BIGINT NOT NULL COMMENT '脱敏规则ID',
    `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
    `category_id` BIGINT NOT NULL COMMENT '绑定数据分类ID',
    `algorithm_type` VARCHAR(32) NOT NULL COMMENT '算法类型(MASK/HASH/CRYPTO/SPECIAL)',
    `algorithm_config` TEXT DEFAULT NULL COMMENT '掩码参数配置(JSON)',
    `algorithm_params` TEXT DEFAULT NULL COMMENT '掩码参数(JSON)',
    `key_id` BIGINT DEFAULT NULL COMMENT '关联密钥ID',
    `scope_type` VARCHAR(32) NOT NULL DEFAULT 'GLOBAL' COMMENT '生效范围(GLOBAL/SPECIFIC_DATASOURCE/SPECIFIC_PROJECT)',
    `scope_target` TEXT DEFAULT NULL COMMENT '范围目标明细(JSON)',
    `apply_scene` VARCHAR(32) NOT NULL DEFAULT 'QUERY_DISPLAY' COMMENT '应用场景(QUERY_DISPLAY/WRITE_STORAGE)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_mask_cat` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脱敏规则配置表';

-- 10. 动态脱敏时效白名单表
CREATE TABLE IF NOT EXISTS `sec_masking_whitelist` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '白名单ID',
    `whitelist_name` VARCHAR(128) NOT NULL DEFAULT '时效免脱敏白名单' COMMENT '白名单名称',
    `grantee_type` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '授权主体类型(USER/ROLE)',
    `grantee_id` VARCHAR(64) NOT NULL COMMENT '主体ID',
    `category_id` BIGINT NOT NULL COMMENT '关联数据分类ID',
    `rule_id` BIGINT NOT NULL COMMENT '关联脱敏规则ID',
    `start_time` DATETIME NOT NULL COMMENT '生效开始时间',
    `end_time` DATETIME NOT NULL COMMENT '生效截止时间',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/EXPIRED/REVOKED)',
    `reason` VARCHAR(1024) NOT NULL COMMENT '申请原因',
    `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_wl_grantee` (`grantee_type`, `grantee_id`),
    KEY `idx_sec_wl_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态脱敏时效白名单表';

-- 11. 安全审计日志表
CREATE TABLE IF NOT EXISTS `sec_security_audit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
    `action_type` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
    `client_ip` VARCHAR(64) NOT NULL DEFAULT '127.0.0.1' COMMENT '客户端IP',
    `target_resource` VARCHAR(256) NOT NULL COMMENT '操作对象资源',
    `action_detail` TEXT DEFAULT NULL COMMENT '详细操作快照(JSON)',
    `risk_level` VARCHAR(32) NOT NULL DEFAULT 'LOW' COMMENT '风险级别(LOW/MEDIUM/HIGH/CRITICAL)',
    `occurred_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sec_audit_action` (`action_type`),
    KEY `idx_sec_audit_time` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全审计日志表';

-- 12. 预置内置识别特征数据
INSERT IGNORE INTO `sec_sensitive_rule` (`id`, `rule_name`, `rule_type`, `description`, `priority`, `owner`, `status`, `category_scope_mode`, `scan_scope_type`, `feature_config`, `tagged_fields_count`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES
(1001, '性别', 'BUILTIN', '用于识别男女、性别相关字段', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1001","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1001_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(gender|sex|性别).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1002, 'IP(v4)地址', 'BUILTIN', '识别标准IPv4互联网协议地址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1002","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1002_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1003, 'URL地址', 'BUILTIN', '识别标准HTTP/HTTPS网址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1003","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1003_1","type":"LEAF","field":"CONTENT","operator":"REGEX_CASE_INSENSITIVE","value":"^https?://.*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1004, '经营许可证', 'BUILTIN', '企业经营与业务资质许可证号识别', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1004","type":"GROUP","logicalOp":"OR","children":[{"id":"leaf_1004_1","type":"LEAF","field":"COLUMN_NAME","operator":"CONTAINS","value":"license"},{"id":"leaf_1004_2","type":"LEAF","field":"COLUMN_COMMENT","operator":"CONTAINS","value":"经营许可证"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1005, '车辆种类', 'BUILTIN', '机动车及营运车辆类型标识', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1005","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1005_1","type":"LEAF","field":"COLUMN_COMMENT","operator":"CONTAINS","value":"车辆种类"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1006, '交易金额', 'BUILTIN', '识别金融交易、结算及账户金额相关数值字段', 30, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1006","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1006_1","type":"LEAF","field":"DATA_TYPE","operator":"IN_LIST","value":["decimal","bigint","int"]},{"id":"leaf_1006_2","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(amount|balance|fee|amt|money|金额|余额).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1007, '中文姓名', 'BUILTIN', '识别2~4位常见中文汉字姓名', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1007","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1007_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(user_name|cust_name|real_name|name|姓名|客户名).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1008, '城市', 'BUILTIN', '国内及国际城市与区域名称', 25, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1008","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1008_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(city|城市).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1009, '证件类型', 'BUILTIN', '身份证、护照、港澳通行证等证件类别编码', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1009","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1009_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(cert_type|id_type|证件类型).*"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1010, '身份证号(中国台湾)', 'BUILTIN', '中国台湾地区10位身分证字号格式识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1010","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1010_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[A-Z][12]\\\\d{8}$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1011, '居民身份证(中国大陆)', 'BUILTIN', '中国大陆18位第二代居民身份证号码识别', 5, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1011","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1011_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[1-9]\\\\d{5}(18|19|20)\\\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\\\d{3}[0-9Xx]$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1012, '移动电话', 'BUILTIN', '中国大陆11位手机号码识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1012","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1012_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^1[3-9]\\\\d{9}$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1013, '电子邮箱', 'BUILTIN', '标准Email电子邮箱格式', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1013","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1013_1","type":"LEAF","field":"CONTENT","operator":"REGEX_CASE_INSENSITIVE","value":"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,6}$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1014, '银行卡号', 'BUILTIN', '国内16~19位银联借记卡与信用卡号', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1014","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1014_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[1-9]\\\\d{15,18}$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10'),
(1015, '统一社会信用代码', 'BUILTIN', '中国大陆18位法人和其他组织统一社会信用代码', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{"id":"root_1015","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1015_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[0-9A-HJ-NPQRTUWXY]{2}\\\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10');
