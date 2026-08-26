package com.yss.datasecurity.domain.constant;

/**
 * 敏感数据识别规则常量类
 */
public final class RecognitionRuleConstants {

    private RecognitionRuleConstants() {}

    /** 规则名称最大允许字符长度 */
    public static final int MAX_RULE_NAME_LENGTH = 12;

    /** 规则说明最大允许字符长度 */
    public static final int MAX_DESCRIPTION_LENGTH = 128;

    /** 过滤条件最大限制数 */
    public static final int MAX_CONDITION_COUNT = 10;

    /** 过滤条件最大嵌套层级 */
    public static final int MAX_NESTED_LEVEL = 2;

    /** 标签多选最大数量 */
    public static final int MAX_TAG_SELECTION_COUNT = 500;

    /** 规则优先级最大值（优先级最低） */
    public static final int MAX_PRIORITY = 100;

    /** 规则优先级最小值（优先级最高） */
    public static final int MIN_PRIORITY = 1;

    /** 默认规则优先级 */
    public static final int DEFAULT_RULE_PRIORITY = 10;

    /** 仲裁算法：规则优先级权重放大系数 */
    public static final int ARBITRATION_PRIORITY_FACTOR = 10;

    /** 仲裁算法：密级敏感度权重放大系数 */
    public static final int ARBITRATION_SENSITIVITY_FACTOR = 5;

    /** 仲裁算法：默认基础置信度 */
    public static final double DEFAULT_BASE_CONFIDENCE = 80.0;

    /** 仲裁算法：推荐更优匹配置信度 */
    public static final double RECOMMENDED_MATCH_CONFIDENCE = 99.5;
}
