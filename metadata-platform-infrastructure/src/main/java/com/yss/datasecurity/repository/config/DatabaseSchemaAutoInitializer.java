package com.yss.datasecurity.repository.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
public class DatabaseSchemaAutoInitializer implements BeanPostProcessor {

    private boolean initialized = false;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !initialized) {
            initialized = true;
            initDatabaseSchema((DataSource) bean);
        }
        return bean;
    }

    private synchronized void initDatabaseSchema(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            log.info("Checking and auto-initializing leaf_alloc table for YSS distributed ID...");

            // 1. 确保 leaf_alloc 表在 SegmentService 初始化前已就绪
            stmt.execute("CREATE TABLE IF NOT EXISTS `leaf_alloc` (" +
                    "`biz_tag` VARCHAR(128) NOT NULL COMMENT '业务标识/表名'," +
                    "`max_id` BIGINT NOT NULL DEFAULT 10000 COMMENT '当前已分配最大ID'," +
                    "`step` INT NOT NULL DEFAULT 2000 COMMENT '号段步长'," +
                    "`description` VARCHAR(256) DEFAULT NULL COMMENT '业务描述'," +
                    "`update_time` VARCHAR(64) DEFAULT NULL COMMENT '更新时间'," +
                    "PRIMARY KEY (`biz_tag`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Leaf 号段模式发号配置表'");

            // 2. 预置业务表 Tag
            stmt.execute("INSERT IGNORE INTO `leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES " +
                    "('default', 10000, 2000, '默认全局发号段', '2026-08-16 20:00:00')," +
                    "('sec_security_grade', 1000, 500, '安全分级主键发号', '2026-08-16 20:00:00')," +
                    "('sec_category_tree', 5000, 1000, '分类目录树节点主键发号', '2026-08-16 20:00:00')," +
                    "('sec_data_category', 10000, 2000, '数据分类主键发号', '2026-08-16 20:00:00')," +
                    "('sec_sensitive_rule', 10000, 2000, '识别规则主键发号', '2026-08-16 20:00:00')," +
                    "('sec_recognition_rule', 10000, 2000, '识别规则主键发号', '2026-08-24 20:00:00')," +
                    "('sec_sensitive_record', 100000, 10000, '敏感打标资产记录发号', '2026-08-16 20:00:00')," +
                    "('sec_key_secret', 10000, 2000, '安全密钥主键发号', '2026-08-16 20:00:00')," +
                    "('sec_masking_rule', 10000, 2000, '脱敏规则主键发号', '2026-08-16 20:00:00')," +
                    "('sec_masking_whitelist', 10000, 2000, '脱敏时效白名单主键发号', '2026-08-16 20:00:00')," +
                    "('sec_security_audit', 100000, 10000, '安全审计日志主键发号', '2026-08-16 20:00:00')");

            // 3. 执行全量 schema.sql 自动创建业务表与基础数据
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("db/schema.sql"));
                populator.setContinueOnError(true);
                populator.execute(dataSource);
                log.info("Database schema.sql initialized successfully.");
            } catch (Exception se) {
                log.debug("schema.sql auto populator notice: {}", se.getMessage());
            }

            // 4. 自动热升级已有表缺失的字段 (Self-healing migration for existing databases)
            upgradeExistingTables(stmt);

            log.info("Leaf distributed-id tables and schema self-healing completed successfully.");
        } catch (Exception e) {
            log.warn("Database schema auto-initializer notice: {}", e.getMessage());
        }
    }

    private void upgradeExistingTables(Statement stmt) {
        String[] patchSqls = new String[] {
            "ALTER TABLE `sec_sensitive_rule` ADD COLUMN `tagged_fields_count` INT NOT NULL DEFAULT 0 COMMENT '已打标字段数'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `scan_dimension_config` TEXT DEFAULT NULL COMMENT '6维高级扫描特征配置'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `disable_policy` VARCHAR(32) NOT NULL DEFAULT 'RETAIN_TAGS' COMMENT '停用策略'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `referenced_rules_count` INT NOT NULL DEFAULT 0 COMMENT '引用脱敏规则数'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `public_key_value` TEXT DEFAULT NULL COMMENT '非对称公钥明文'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `sample_preview` VARCHAR(1024) DEFAULT NULL COMMENT '敏感数据样例脱敏预览'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `confidence_score` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `algorithm_config` TEXT DEFAULT NULL COMMENT '掩码参数配置(JSON)'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `algorithm_params` TEXT DEFAULT NULL COMMENT '掩码参数(JSON)'",
            "ALTER TABLE `sec_masking_rule` MODIFY COLUMN `algorithm_params` TEXT DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_rule` MODIFY COLUMN `algorithm_config` TEXT DEFAULT NULL COMMENT '掩码参数配置(JSON)'",
            "ALTER TABLE `sec_masking_rule` MODIFY COLUMN `scope_target` TEXT DEFAULT NULL COMMENT '范围目标明细(JSON)'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `scope_target` TEXT DEFAULT NULL COMMENT '范围目标明细(JSON)'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `scope_type` VARCHAR(32) NOT NULL DEFAULT 'GLOBAL' COMMENT '生效范围'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `apply_scene` VARCHAR(32) NOT NULL DEFAULT 'QUERY_DISPLAY' COMMENT '应用场景'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `key_id` BIGINT DEFAULT NULL COMMENT '关联密钥ID'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `whitelist_name` VARCHAR(128) NOT NULL DEFAULT '时效免脱敏白名单' COMMENT '白名单名称'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `whitelist_name` VARCHAR(128) NOT NULL DEFAULT '时效免脱敏白名单' COMMENT '白名单名称'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `subject_id` VARCHAR(64) DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `subject_name` VARCHAR(128) DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `subject_type` VARCHAR(32) DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `valid_start_time` DATETIME DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `valid_end_time` DATETIME DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` MODIFY COLUMN `masking_rule_id` BIGINT DEFAULT NULL COMMENT '历史兼容字段'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `grantee_type` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '授权主体类型(USER/ROLE/APP)'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `grantee_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '主体标识(用户ID/角色编码)'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT '关联数据分类ID'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `rule_id` BIGINT DEFAULT NULL COMMENT '关联脱敏规则ID'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效开始时间'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `end_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效截止时间'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/EXPIRED/REVOKED)'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `reason` VARCHAR(1024) NOT NULL DEFAULT '业务申请' COMMENT '申请原因'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_masking_whitelist` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `description` VARCHAR(512) DEFAULT NULL COMMENT '规则脱敏说明'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `mask_method` VARCHAR(32) NOT NULL DEFAULT 'UNDERLYING' COMMENT '脱敏方式(UNDERLYING/DISPLAY)'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `plate_scope` VARCHAR(256) NOT NULL DEFAULT 'ALL' COMMENT '所属板块'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `project_scope` VARCHAR(256) NOT NULL DEFAULT 'ALL' COMMENT '所属项目'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `sub_algorithm` VARCHAR(64) DEFAULT NULL COMMENT '子算法'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `owner` VARCHAR(64) NOT NULL DEFAULT '安全管理员' COMMENT '负责人'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_masking_rule` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_data_category` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_category_tree` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_category_tree` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_category_tree` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_category_tree` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_security_grade` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_security_grade` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_security_grade` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_security_grade` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `created_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `key_length` INT DEFAULT NULL COMMENT '密钥长度(位)'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `gen_type` VARCHAR(32) NOT NULL DEFAULT 'SYSTEM' COMMENT '生成方式(SYSTEM/CUSTOM)'",
            "ALTER TABLE `sec_key_secret` ADD COLUMN `owner_only` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否仅负责人管理'",
            "CREATE TABLE IF NOT EXISTS `sec_key_permission` (" +
                    "`id` BIGINT NOT NULL COMMENT '主键ID'," +
                    "`key_id` BIGINT NOT NULL COMMENT '密钥ID'," +
                    "`grantee_type` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '授权主体类型(USER/ROLE)'," +
                    "`grantee_id` VARCHAR(64) NOT NULL COMMENT '主体ID'," +
                    "`grantee_name` VARCHAR(128) NOT NULL COMMENT '主体名称'," +
                    "`permission_type` VARCHAR(32) NOT NULL DEFAULT 'USE' COMMENT '权限类型(USE/MANAGE)'," +
                    "`granted_by` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '授权人'," +
                    "`granted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_key_perm_key_id` (`key_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥权限授权表'",
            "CREATE TABLE IF NOT EXISTS `sec_key_task_reference` (" +
                    "`id` BIGINT NOT NULL COMMENT '主键ID'," +
                    "`key_id` BIGINT NOT NULL COMMENT '密钥ID'," +
                    "`task_name` VARCHAR(128) NOT NULL COMMENT '任务名称'," +
                    "`sector_name` VARCHAR(64) NOT NULL DEFAULT '资管板块' COMMENT '数据板块'," +
                    "`project_name` VARCHAR(64) NOT NULL DEFAULT '核心交易项目' COMMENT '所属项目'," +
                    "`task_type` VARCHAR(32) NOT NULL DEFAULT 'DYNAMIC_MASK' COMMENT '任务类型'," +
                    "`operation_type` VARCHAR(32) NOT NULL DEFAULT 'DECRYPT' COMMENT '操作类型'," +
                    "`owner` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '负责人'," +
                    "`last_executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近执行时间'," +
                    "PRIMARY KEY (`id`)," +
                    "KEY `idx_key_task_key_id` (`key_id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥任务引用记录表'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `updated_by` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
            "ALTER TABLE `sec_sensitive_rule` ADD COLUMN `rule_type` VARCHAR(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '规则类型(BUILTIN/CUSTOM)'",
            "ALTER TABLE `sec_sensitive_rule` MODIFY COLUMN `rule_name` VARCHAR(128) NOT NULL COMMENT '特征名称'",
            "ALTER TABLE `sec_sensitive_rule` MODIFY COLUMN `description` VARCHAR(1024) DEFAULT NULL COMMENT '特征描述'",
            "CREATE TABLE IF NOT EXISTS `sec_recognition_rule` (" +
                    "`id` BIGINT NOT NULL COMMENT '识别规则ID'," +
                    "`rule_name` VARCHAR(32) NOT NULL COMMENT '识别规则名称'," +
                    "`description` VARCHAR(256) DEFAULT NULL COMMENT '识别规则说明'," +
                    "`category_scope_mode` VARCHAR(32) NOT NULL DEFAULT 'ALL' COMMENT '数据分类圈选模式'," +
                    "`category_scope_config` TEXT DEFAULT NULL COMMENT '数据分类圈选配置(JSON)'," +
                    "`scan_source_type` VARCHAR(32) NOT NULL DEFAULT 'COMPUTE_ENGINE' COMMENT '扫描数据来源'," +
                    "`compute_scope_config` TEXT DEFAULT NULL COMMENT '计算源表扫描范围配置(JSON)'," +
                    "`datasource_scope_config` TEXT DEFAULT NULL COMMENT '数据源表扫描范围配置(JSON)'," +
                    "`owner` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '负责人'," +
                    "`status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '是否生效'," +
                    "`priority` INT NOT NULL DEFAULT 50 COMMENT '规则优先级'," +
                    "`tagged_fields_count` INT NOT NULL DEFAULT 0 COMMENT '已打标字段数'," +
                    "`lineage_inheritance_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开启基于血缘自动继承'," +
                    "`created_by` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '创建人'," +
                    "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "`updated_by` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '更新人'," +
                    "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `uk_sec_rec_rule_name` (`rule_name`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感数据识别规则表'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `masking_status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '脱敏生效状态(ENABLED/DISABLED)'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `masking_status_updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '脱敏状态变更时间'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `recognition_method` VARCHAR(32) NOT NULL DEFAULT 'AUTO' COMMENT '识别方式(AUTO/MANUAL/LINEAGE)'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `asset_source_type` VARCHAR(32) NOT NULL DEFAULT 'DATAPHIN' COMMENT '资产来源类型(DATAPHIN/DATASOURCE)'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `asset_source_info` VARCHAR(512) DEFAULT NULL COMMENT '资产来源信息(所属项目/板块或数据源/Schema)'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `recommended_category_id` BIGINT DEFAULT NULL COMMENT '推荐分类ID'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `recommended_category_name` VARCHAR(512) DEFAULT NULL COMMENT '推荐分类名称'",
            "ALTER TABLE `sec_sensitive_record` ADD COLUMN `has_better_recommendation` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否存在更优推荐'",
            "CREATE TABLE IF NOT EXISTS `sec_import_batch_log` (" +
                    "`id` BIGINT NOT NULL COMMENT '导入批次ID'," +
                    "`batch_type` VARCHAR(64) NOT NULL DEFAULT 'IMPORT' COMMENT '操作类型(IMPORT/MANUAL_ADD/BATCH_EDIT)'," +
                    "`file_name` VARCHAR(256) DEFAULT NULL COMMENT '文件名'," +
                    "`asset_type` VARCHAR(32) NOT NULL DEFAULT 'DATAPHIN' COMMENT '资产类型'," +
                    "`total_count` INT NOT NULL DEFAULT 0 COMMENT '总条数'," +
                    "`success_count` INT NOT NULL DEFAULT 0 COMMENT '成功条数'," +
                    "`failed_count` INT NOT NULL DEFAULT 0 COMMENT '失败条数'," +
                    "`conflict_strategy` VARCHAR(64) DEFAULT NULL COMMENT '去重/冲突策略'," +
                    "`masking_policy` VARCHAR(64) DEFAULT NULL COMMENT '脱敏策略'," +
                    "`status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态(SUCCESS/PARTIAL_FAILED/FAILED)'," +
                    "`error_report_url` VARCHAR(512) DEFAULT NULL COMMENT '错误报告地址'," +
                    "`operator` VARCHAR(64) NOT NULL DEFAULT 'admin' COMMENT '操作人'," +
                    "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'," +
                    "PRIMARY KEY (`id`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识别结果批量操作与导入历史表'"
        };

        for (String sql : patchSqls) {
            try {
                stmt.execute(sql);
            } catch (Exception ignored) {
                // 字段已存在时忽略异常
            }
        }

        seedBuiltinRules(stmt);
        seedRecognitionRules(stmt);
        seedRecognitionResults(stmt);
        seedKeySecrets(stmt);
    }

    private void seedKeySecrets(Statement stmt) {
        try {
            stmt.execute("INSERT IGNORE INTO `sec_key_secret` (`id`, `key_name`, `key_type`, `algorithm`, `key_length`, `gen_type`, `owner_only`, `encrypted_key_value`, `public_key_value`, `owner`, `description`, `status`, `referenced_rules_count`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES " +
                    "(9001, 'EDS密钥', 'HASH', '-', NULL, 'CUSTOM', 0, 'ENC(v1:RWRTX1NhbHRfS2V5XzIwMjQ=)', NULL, 'zhangjianteng', '用于EDS加解密。', 'ACTIVE', 1, 'zhangjianteng', '2024-01-04 09:28:51', 'zhangjianteng', '2024-01-04 09:28:51')," +
                    "(9002, '生产客户AES主密钥', 'ENCRYPTION', 'AES', 256, 'SYSTEM', 1, 'ENC(v1:YTBjMWQyZTNmNGc1aDZsN204bjlvMHBxcnN0dXZ3eHk=)', NULL, 'admin', '核心生产库客户手机与证件号字段AES-256加解密', 'ACTIVE', 3, 'admin', '2024-01-08 14:15:30', 'admin', '2024-01-08 14:15:30')," +
                    "(9003, '国密SM4数据主密钥', 'ENCRYPTION', 'SM4', 128, 'SYSTEM', 0, 'ENC(v1:U000X1NlY3JldEtleV8xMjhiaXRfc3Ryb25n)', NULL, 'sec_admin', '金融国密合规合规SM4-128加密', 'ACTIVE', 2, 'sec_admin', '2024-01-12 10:20:00', 'sec_admin', '2024-01-12 10:20:00')," +
                    "(9004, '交易签名SM2密钥对', 'ENCRYPTION', 'SM2', NULL, 'CUSTOM', 1, 'ENC(v1:U00yX1ByaXZhdGVLZXlfMDQzOWFjZWZkZg==)', '04B8E17A883940192A0284DE0983A12908320491823901842091', 'liufeng', 'SM2国密非对称密钥对，用于核心签名与验签', 'ACTIVE', 0, 'liufeng', '2024-01-15 16:45:12', 'liufeng', '2024-01-15 16:45:12')," +
                    "(9005, '手机号FPE保留格式密钥', 'ENCRYPTION', 'FF1', 128, 'SYSTEM', 0, 'ENC(v1:RlBFX0ZGMTIxOGtleV9zZWN1cmVfcHJvZA==)', NULL, 'wangwu', '用于手机号与银行卡保留格式原生加密', 'ACTIVE', 1, 'wangwu', '2024-01-18 11:30:20', 'wangwu', '2024-01-18 11:30:20')");

            stmt.execute("INSERT IGNORE INTO `sec_key_task_reference` (`id`, `key_id`, `task_name`, `sector_name`, `project_name`, `task_type`, `operation_type`, `owner`, `last_executed_at`) VALUES " +
                    "(9101, 9001, 'EDS客户历史敏感数据归档加密', '资管板块', 'EDS归档项目', 'DYNAMIC_MASK', 'ENCRYPT', 'zhangjianteng', '2024-01-04 10:00:00')," +
                    "(9102, 9002, '生产核心客户信息动态脱敏规则', '核心交易板块', '投研风控项目', 'DYNAMIC_MASK', 'ENCRYPT', 'admin', '2024-01-08 15:00:00')," +
                    "(9103, 9002, '数仓ODS层客户证件号解密任务', '资管板块', '数仓中台项目', 'DATA_INTEGRATION', 'DECRYPT', 'admin', '2024-01-08 16:20:00')," +
                    "(9104, 9003, '国密SM4银行卡结算批量加密', '清算板块', '核心清算项目', 'STATIC_MASK', 'ENCRYPT', 'sec_admin', '2024-01-12 11:00:00')");

            stmt.execute("INSERT IGNORE INTO `sec_key_permission` (`id`, `key_id`, `grantee_type`, `grantee_id`, `grantee_name`, `permission_type`, `granted_by`, `granted_at`) VALUES " +
                    "(9201, 9001, 'USER', 'zhangjianteng', '张建腾 (密钥负责人)', 'MANAGE', 'system', '2024-01-04 09:28:51')," +
                    "(9202, 9001, 'ROLE', 'ROLE_SEC_ADMIN', '安全管理员角色', 'USE', 'admin', '2024-01-04 10:30:00')," +
                    "(9203, 9002, 'USER', 'admin', '超级管理员', 'MANAGE', 'system', '2024-01-08 14:15:30')," +
                    "(9204, 9002, 'ROLE', 'ROLE_DATA_DEV', '数据开发工程师', 'USE', 'admin', '2024-01-08 14:30:00')");
        } catch (Exception e) {
            log.debug("seedKeySecrets notice: {}", e.getMessage());
        }
    }

    private void seedRecognitionRules(Statement stmt) {
        try {
            stmt.execute("INSERT IGNORE INTO `sec_recognition_rule` (`id`, `rule_name`, `description`, `category_scope_mode`, `category_scope_config`, `scan_source_type`, `compute_scope_config`, `datasource_scope_config`, `owner`, `status`, `priority`, `tagged_fields_count`, `lineage_inheritance_enabled`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES " +
                    "(2001, '客户身份识别', '全库识别客户身份证、手机号与姓名', 'ALL', '[]', 'COMPUTE_ENGINE', '{\"logic\":\"AND\",\"rules\":[{\"type\":\"SECTOR\",\"condition\":\"ALL\",\"values\":[]}]}', NULL, 'admin', 'ENABLED', 10, 1420, 1, 'admin', '2026-08-20 10:00:00', 'admin', '2026-08-24 15:30:00')," +
                    "(2002, '交易财务扫描', '针对核心结算与财务库表扫描银行卡及交易金额', 'SPECIFIC', '[{\"treeNodeId\":2,\"categoryIds\":[1006,1014]}]', 'DATASOURCE', NULL, '{\"datasourceIds\":[\"mysql_prod_01\"],\"rangeType\":\"SPECIFIC_TABLES\",\"logic\":\"AND\",\"conditions\":[{\"field\":\"TABLE_NAME\",\"condition\":\"CONTAINS\",\"values\":[\"trade\",\"order\",\"pay\"]},{\"field\":\"TAG\",\"condition\":\"CONTAINS_ANY\",\"values\":[\"核心资产\"]}]}', 'admin', 'ENABLED', 20, 860, 0, 'admin', '2026-08-21 14:20:00', 'admin', '2026-08-24 16:00:00')," +
                    "(2003, '公网IP合规', '识别系统网络与访问日志中的IP和URL', 'TREE_NODE', '[{\"treeNodeId\":3}]', 'COMPUTE_ENGINE', '{\"logic\":\"OR\",\"rules\":[{\"type\":\"PROJECT\",\"condition\":\"CONTAINS\",\"values\":[\"log_center\",\"gateway\"]},{\"type\":\"TABLE\",\"condition\":\"REGEX\",\"values\":[\".*access_log.*\"]}]}', NULL, 'sec_auditor', 'DISABLED', 30, 310, 0, 'sec_auditor', '2026-08-22 09:15:00', 'sec_auditor', '2026-08-24 12:00:00')");
        } catch (Exception e) {
            log.debug("seedRecognitionRules notice: {}", e.getMessage());
        }
    }

    private void seedBuiltinRules(Statement stmt) {
        try {
            stmt.execute("INSERT IGNORE INTO `sec_sensitive_rule` (`id`, `rule_name`, `rule_type`, `description`, `priority`, `owner`, `status`, `category_scope_mode`, `scan_scope_type`, `feature_config`, `tagged_fields_count`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES " +
                    "(1001, '性别', 'BUILTIN', '用于识别男女、性别相关字段', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\".*(gender|sex|性别).*\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1002, 'IP(v4)地址', 'BUILTIN', '识别标准IPv4互联网协议地址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1003, 'URL地址', 'BUILTIN', '识别标准HTTP/HTTPS网址', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\"^https?://.*\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1004, '经营许可证', 'BUILTIN', '企业经营与业务资质许可证号识别', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"OR\",\"rules\":[{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"CONTAINS\",\"value\":\"license\"},{\"scanType\":\"COLUMN_COMMENT\",\"matchMode\":\"CONTAINS\",\"value\":\"经营许可证\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1005, '车辆种类', 'BUILTIN', '机动车及营运车辆类型标识', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"COLUMN_COMMENT\",\"matchMode\":\"CONTAINS\",\"value\":\"车辆种类\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1006, '交易金额', 'BUILTIN', '识别金融交易、结算及账户金额相关数值字段', 30, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"DATA_TYPE\",\"matchMode\":\"IN_LIST\",\"dataTypes\":[\"decimal\",\"bigint\",\"int\"]},{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\".*(amount|balance|fee|amt|money|金额|余额).*\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1007, '中文姓名', 'BUILTIN', '识别2~4位常见中文汉字姓名', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\".*(user_name|cust_name|real_name|name|姓名|客户名).*\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1008, '城市', 'BUILTIN', '国内及国际城市与区域名称', 25, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\".*(city|城市).*\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1009, '证件类型', 'BUILTIN', '身份证、护照、港澳通行证等证件类别编码', 20, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"COLUMN_NAME\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\".*(cert_type|id_type|证件类型).*\"}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1010, '身份证号(中国台湾)', 'BUILTIN', '中国台湾地区10位身分证字号格式识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^[A-Z][12]\\\\d{8}$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1011, '居民身份证(中国大陆)', 'BUILTIN', '中国大陆18位第二代居民身份证号码识别', 5, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^[1-9]\\\\d{5}(18|19|20)\\\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\\\d{3}[0-9Xx]$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1012, '移动电话', 'BUILTIN', '中国大陆11位手机号码识别', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^1[3-9]\\\\d{9}$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1013, '电子邮箱', 'BUILTIN', '标准Email电子邮箱格式', 15, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_CASE_INSENSITIVE\",\"value\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,6}$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1014, '银行卡号', 'BUILTIN', '国内16~19位银联借记卡与信用卡号', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^[1-9]\\\\d{15,18}$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')," +
                    "(1015, '统一社会信用代码', 'BUILTIN', '中国大陆18位法人和其他组织统一社会信用代码', 10, 'system', 'ENABLED', 'ALL', 'DATASOURCE', '{\"logic\":\"AND\",\"rules\":[{\"scanType\":\"CONTENT\",\"matchMode\":\"REGEX_EXACT\",\"value\":\"^[0-9A-HJ-NPQRTUWXY]{2}\\\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$\",\"threshold\":80}]}', 0, 'system', '2025-02-05 17:13:10', 'system', '2025-02-05 17:13:10')");
        } catch (Exception e) {
            log.debug("seedBuiltinRules notice: {}", e.getMessage());
        }
    }

    private void seedRecognitionResults(Statement stmt) {
        try {
            stmt.execute("INSERT IGNORE INTO `sec_sensitive_record` (`id`, `datasource_id`, `datasource_name`, `schema_name`, `table_name`, `field_name`, `field_comment`, `category_id`, `category_name`, `security_grade_id`, `security_grade_name`, `sensitivity_score`, `matched_rule_id`, `matched_rule_name`, `source_type`, `is_locked`, `lock_user`, `lock_time`, `sample_data`, `sample_preview`, `confidence_score`, `status`, `masking_status`, `masking_status_updated_at`, `recognition_method`, `asset_source_type`, `asset_source_info`, `recommended_category_id`, `recommended_category_name`, `has_better_recommendation`, `created_at`, `updated_by`, `updated_at`) VALUES " +
                    "(3001, 'dataphin_fashion_cdm', 'fashion_cdm_dev', 'LD_Fashion_dev', 'fct_pay_order_di', 'pay_order_no', '支付订单号', 1006, '订单信息 (/交易信息/)', 2, 'L2', 2, 2002, '交易财务扫描', 'MANUAL_LOCKED', 1, 'admin', '2025-03-17 19:29:54', 'PO2025031700928371', 'PO20250317****8371', 98.50, 'CONFIRMED', 'ENABLED', '2025-03-17 19:29:54', 'MANUAL', 'DATAPHIN', 'fashion_cdm_dev (服饰CDM项目) / LD_Fashion_dev (服饰零售_开发)', NULL, NULL, 0, '2025-03-17 19:29:54', 'admin', '2025-03-17 19:29:54')," +
                    "(3002, 'dataphin_fashion_ods', 'fashion_ods_dev', 'LD_Fashion_dev', 'character_sets', 'default_collate_name', '默认字符集排序规则', 1001, '-测试 (/个人信息/个人基本信息/)', 3, 'L3', 3, 2001, '客户身份识别', 'RULE_AUTO', 0, NULL, NULL, 'utf8mb4_general_ci', 'utf8mb4_general_ci', 85.00, 'UNCONFIRMED', 'ENABLED', '2025-03-17 16:48:24', 'AUTO', 'DATAPHIN', 'fashion_ods_dev (服饰ODS项目) / LD_Fashion_dev (服饰零售_开发)', 1007, '中文姓名 (/个人信息/个人基本信息/)', 1, '2025-03-17 16:48:24', 'system', '2025-03-17 16:48:24')," +
                    "(3003, 'dataphin_fashion_ods', 'fashion_ods_dev', 'LD_Fashion_dev', 'character_sets', 'character_set_name', '字符集名称', 1001, '-测试 (/个人信息/个人基本信息/)', 3, 'L3', 3, 2001, '客户身份识别', 'RULE_AUTO', 0, NULL, NULL, 'utf8mb4', 'utf8mb4', 85.00, 'UNCONFIRMED', 'ENABLED', '2025-03-17 16:48:24', 'AUTO', 'DATAPHIN', 'fashion_ods_dev (服饰ODS项目) / LD_Fashion_dev (服饰零售_开发)', NULL, NULL, 0, '2025-03-17 16:48:24', 'system', '2025-03-17 16:48:24')," +
                    "(3004, 'dataphin_ranzhou', 'ranzhou_test_project', 'LD_bus', 'columns_priv', 'table_name', '授权表名', 1007, '姓名 (/公交集团/)', 3, 'L3', 3, 2001, '客户身份识别', 'RULE_AUTO', 0, NULL, NULL, 'bus_driver_info', 'bus_driver_info', 92.00, 'UNCONFIRMED', 'ENABLED', '2025-03-17 16:08:07', 'AUTO', 'DATAPHIN', 'ranzhou_test_project (然洲测试项目) / LD_bus (城投)', NULL, NULL, 0, '2025-03-17 16:08:07', 'system', '2025-03-17 16:08:07')," +
                    "(3005, 'dataphin_ranzhou', 'ranzhou_test_project', 'LD_bus', 'columns_priv', 'column_name', '授权列名', 1007, '姓名 (/公交集团/)', 3, 'L3', 3, 2001, '客户身份识别', 'RULE_AUTO', 0, NULL, NULL, 'driver_name', 'driver_name', 92.00, 'UNCONFIRMED', 'ENABLED', '2025-03-17 16:08:07', 'AUTO', 'DATAPHIN', 'ranzhou_test_project (然洲测试项目) / LD_bus (城投)', NULL, NULL, 0, '2025-03-17 16:08:07', 'system', '2025-03-17 16:08:07')," +
                    "(3006, 'dataphin_ranzhou_dev', 'ranzhou_test_project_dev', 'LD_bus_dev', 'ods_hzct_user_info', 'id', '用户主键ID', 1006, '订单金额 (/城投/)', 3, 'L3', 3, 2002, '交易财务扫描', 'RULE_AUTO', 0, NULL, NULL, '100827162981', '100827162981', 88.00, 'UNCONFIRMED', 'ENABLED', '2025-03-16 17:53:28', 'AUTO', 'DATAPHIN', 'ranzhou_test_project_dev (然洲测试开发) / LD_bus_dev (城投_开发)', 1011, '居民身份证(中国大陆)', 1, '2025-03-16 17:53:28', 'system', '2025-03-16 17:53:28')," +
                    "(3007, 'dataphin_ranzhou_dev', 'ranzhou_test_project_dev', 'LD_bus_dev', 'ods_hzct_user_info', 'phone', '联系手机号', 1006, '订单金额 (/城投/)', 3, 'L3', 3, 2002, '交易财务扫描', 'RULE_AUTO', 0, NULL, NULL, '13812345678', '138****5678', 99.00, 'UNCONFIRMED', 'ENABLED', '2025-03-16 17:53:28', 'AUTO', 'DATAPHIN', 'ranzhou_test_project_dev (然洲测试开发) / LD_bus_dev (城投_开发)', 1012, '移动电话', 1, '2025-03-16 17:53:28', 'system', '2025-03-16 17:53:28')," +
                    "(3008, 'dataphin_ranzhou_dev', 'ranzhou_test_project_dev', 'LD_bus_dev', 'ods_hzct_user_info', 'card_id', '市民卡卡号', 1006, '订单金额 (/城投/)', 3, 'L3', 3, 2002, '交易财务扫描', 'RULE_AUTO', 0, NULL, NULL, '330106199001011234', '330106********1234', 96.00, 'UNCONFIRMED', 'ENABLED', '2025-03-16 17:53:28', 'AUTO', 'DATAPHIN', 'ranzhou_test_project_dev (然洲测试开发) / LD_bus_dev (城投_开发)', 1014, '银行卡号', 1, '2025-03-16 17:53:28', 'system', '2025-03-16 17:53:28')," +
                    "(3009, 'dataphin_ranzhou', 'ranzhou_test_project', 'LD_bus', 'ods_hzct_user_info', 'date_time', '乘车刷卡时间', 1006, '交易日期 (/交易信息/)', 2, 'L2', 2, 2002, '交易财务扫描', 'RULE_AUTO', 0, NULL, NULL, '2025-03-16 13:15:22', '2025-03-16 13:15:22', 91.00, 'UNCONFIRMED', 'ENABLED', '2025-03-16 13:15:22', 'AUTO', 'DATAPHIN', 'ranzhou_test_project (然洲测试项目) / LD_bus (杭州城投)', NULL, NULL, 0, '2025-03-16 13:15:22', 'system', '2025-03-16 13:15:22')");

            stmt.execute("INSERT IGNORE INTO `sec_import_batch_log` (`id`, `batch_type`, `file_name`, `asset_type`, `total_count`, `success_count`, `failed_count`, `conflict_strategy`, `masking_policy`, `status`, `error_report_url`, `operator`, `created_at`) VALUES " +
                    "(8001, 'IMPORT', 'dataphin_sensitive_tagging_v1.xlsx', 'DATAPHIN', 120, 120, 0, 'OVERWRITE_ALL', 'UNIFIED_ENABLED', 'SUCCESS', NULL, 'admin', '2025-03-16 10:00:00')," +
                    "(8002, 'MANUAL_ADD', '按表批量添加(8张表/26个字段)', 'DATAPHIN', 26, 26, 0, 'OVERWRITE_UNLOCKED', 'RETAIN_CONFIG', 'SUCCESS', NULL, 'admin', '2025-03-17 11:20:00')");
        } catch (Exception e) {
            log.debug("seedRecognitionResults notice: {}", e.getMessage());
        }
    }
}
