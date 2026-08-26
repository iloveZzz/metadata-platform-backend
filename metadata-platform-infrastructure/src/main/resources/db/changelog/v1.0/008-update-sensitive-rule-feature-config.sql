-- liquibase formatted sql
-- changeset yss:008-update-sensitive-rule-feature-config dbms:mysql,h2

-- 1001: 性别 (COLUMN_NAME + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1001","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1001_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(gender|sex|性别).*"}]}' WHERE `id` = 1001;

-- 1002: IP(v4)地址 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1002","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1002_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"}]}' WHERE `id` = 1002;

-- 1003: URL地址 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1003","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1003_1","type":"LEAF","field":"CONTENT","operator":"REGEX_CASE_INSENSITIVE","value":"^https?://.*"}]}' WHERE `id` = 1003;

-- 1004: 经营许可证 (OR 关系: 字段名包含 license 或 描述包含 经营许可证)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1004","type":"GROUP","logicalOp":"OR","children":[{"id":"leaf_1004_1","type":"LEAF","field":"COLUMN_NAME","operator":"CONTAINS","value":"license"},{"id":"leaf_1004_2","type":"LEAF","field":"COLUMN_COMMENT","operator":"CONTAINS","value":"经营许可证"}]}' WHERE `id` = 1004;

-- 1005: 车辆种类 (COLUMN_COMMENT + 包含)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1005","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1005_1","type":"LEAF","field":"COLUMN_COMMENT","operator":"CONTAINS","value":"车辆种类"}]}' WHERE `id` = 1005;

-- 1006: 交易金额 (AND 关系: 数据类型属于 decimal/bigint/int + 字段名正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1006","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1006_1","type":"LEAF","field":"DATA_TYPE","operator":"IN_LIST","value":["decimal","bigint","int"]},{"id":"leaf_1006_2","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(amount|balance|fee|amt|money|金额|余额).*"}]}' WHERE `id` = 1006;

-- 1007: 中文姓名 (COLUMN_NAME + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1007","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1007_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(user_name|cust_name|real_name|name|姓名|客户名).*"}]}' WHERE `id` = 1007;

-- 1008: 城市 (COLUMN_NAME + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1008","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1008_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(city|城市).*"}]}' WHERE `id` = 1008;

-- 1009: 证件类型 (COLUMN_NAME + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1009","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1009_1","type":"LEAF","field":"COLUMN_NAME","operator":"REGEX_CASE_INSENSITIVE","value":".*(cert_type|id_type|证件类型).*"}]}' WHERE `id` = 1009;

-- 1010: 身份证号(中国台湾) (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1010","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1010_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[A-Z][12]\\\\d{8}$"}]}' WHERE `id` = 1010;

-- 1011: 居民身份证(中国大陆) (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1011","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1011_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[1-9]\\\\d{5}(18|19|20)\\\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\\\d{3}[0-9Xx]$"}]}' WHERE `id` = 1011;

-- 1012: 移动电话 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1012","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1012_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^1[3-9]\\\\d{9}$"}]}' WHERE `id` = 1012;

-- 1013: 电子邮箱 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1013","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1013_1","type":"LEAF","field":"CONTENT","operator":"REGEX_CASE_INSENSITIVE","value":"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,6}$"}]}' WHERE `id` = 1013;

-- 1014: 银行卡号 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1014","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1014_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[1-9]\\\\d{15,18}$"}]}' WHERE `id` = 1014;

-- 1015: 统一社会信用代码 (CONTENT + 正则)
UPDATE `sec_sensitive_rule` SET `feature_config` = '{"id":"root_1015","type":"GROUP","logicalOp":"AND","children":[{"id":"leaf_1015_1","type":"LEAF","field":"CONTENT","operator":"REGEX_EXACT","value":"^[0-9A-HJ-NPQRTUWXY]{2}\\\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$"}]}' WHERE `id` = 1015;
