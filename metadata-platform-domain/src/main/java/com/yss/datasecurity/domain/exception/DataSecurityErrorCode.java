package com.yss.datasecurity.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 数据安全业务异常错误码枚举
 */
@Getter
@RequiredArgsConstructor
public enum DataSecurityErrorCode {
    RECOGNITION_RULE_NOT_FOUND("RECOGNITION_RULE_NOT_FOUND", "识别规则不存在"),
    RULE_NAME_DUPLICATE("RULE_NAME_DUPLICATE", "识别规则名称已存在"),
    INVALID_RULE_NAME("INVALID_RULE_NAME", "识别规则名称不合法"),
    INVALID_RULE_DESCRIPTION("INVALID_RULE_DESCRIPTION", "识别规则说明不能超过最大字符长度"),
    INVALID_RULE_PRIORITY("INVALID_RULE_PRIORITY", "规则优先级超出合法范围"),
    RESULT_NOT_FOUND("RESULT_NOT_FOUND", "识别结果记录不存在"),
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "数据分类不存在"),
    GRADE_NOT_FOUND("GRADE_NOT_FOUND", "数据分级不存在"),
    NODE_NOT_FOUND("NODE_NOT_FOUND", "目录节点不存在"),
    GRADE_NAME_DUPLICATE("GRADE_NAME_DUPLICATE", "分级名称已存在"),
    GRADE_CODE_DUPLICATE("GRADE_CODE_DUPLICATE", "分级缩写已存在"),
    KEY_NOT_FOUND("KEY_NOT_FOUND", "密钥不存在"),
    KEY_NAME_DUPLICATE("KEY_NAME_DUPLICATE", "密钥名称已存在"),
    KEY_VALUE_EMPTY("KEY_VALUE_EMPTY", "自定义密钥值不能为空"),
    KEY_IN_USE("KEY_IN_USE", "密钥被引用中，不可删除"),
    NO_RECOMMENDATION("NO_RECOMMENDATION", "当前记录无推荐打标结果"),
    RULE_NOT_FOUND("RULE_NOT_FOUND", "规则不存在"),
    BUILTIN_RULE_CANNOT_DELETE("BUILTIN_RULE_CANNOT_DELETE", "内置识别特征受系统保护，不可删除");

    private final String code;
    private final String defaultMessage;
}
