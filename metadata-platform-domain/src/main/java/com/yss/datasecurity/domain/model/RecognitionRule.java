package com.yss.datasecurity.domain.model;

import com.yss.datasecurity.domain.exception.DataSecurityException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRule {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{1,12}$");

    private Long id;
    private String ruleName; // 包含中文、字母、数字、下划线（_），不超过12个字符
    private String description; // 不超过128个字符
    private String categoryScopeMode; // ALL / TREE_NODE / SPECIFIC
    private String categoryScopeConfig; // JSON 存储多组目录与分类圈选
    private String scanSourceType; // COMPUTE_ENGINE / DATASOURCE
    private String computeScopeConfig; // JSON 存储计算源扫描范围 (且/或, 规则<=5, 关系<=2层, 对象<=100)
    private String datasourceScopeConfig; // JSON 存储数据源扫描范围 (全部表/指定表, 过滤条件<=10)
    private String owner; // 负责人
    private String status; // ENABLED / DISABLED
    private Integer priority; // 1~100
    private Integer taggedFieldsCount; // 识别打标生效字段数
    private Boolean lineageInheritanceEnabled; // 是否开启基于血缘自动继承
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public void validate() {
        if (ruleName == null || ruleName.trim().isEmpty()) {
            throw new DataSecurityException("INVALID_RULE_NAME", "识别规则名称不能为空");
        }
        if (!NAME_PATTERN.matcher(ruleName.trim()).matches()) {
            throw new DataSecurityException("INVALID_RULE_NAME", "识别规则名称必须由中文、字母、数字、下划线组成且不超过12个字符");
        }
        if (description != null && description.length() > 128) {
            throw new DataSecurityException("INVALID_RULE_DESCRIPTION", "识别规则说明不能超过128个字符");
        }
        if (categoryScopeMode == null || categoryScopeMode.trim().isEmpty()) {
            categoryScopeMode = "ALL";
        }
        if (scanSourceType == null || scanSourceType.trim().isEmpty()) {
            scanSourceType = "COMPUTE_ENGINE";
        }
        if (status == null || status.trim().isEmpty()) {
            status = "ENABLED";
        }
    }
}
