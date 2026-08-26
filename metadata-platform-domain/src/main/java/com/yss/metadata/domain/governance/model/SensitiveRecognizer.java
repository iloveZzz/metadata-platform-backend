package com.yss.metadata.domain.governance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 敏感分类识别器（FR-016；WU-04-02 候选自动识别引擎）。
 *
 * <p>输入：启用的分类规则 + 待识别列；输出：候选分类列表。
 * 内置规则（手机号/身份证/银行卡/邮箱）按列名/注释关键字匹配 →
 * 候选「敏感-PII」（level PII）；自定义规则（正则/列名/字典）按模式匹配 →
 * 候选「敏感」（level SENSITIVE）。命中多条取首个（按规则序），0 命中返回空列表（空结构非错误）。</p>
 *
 * <p>受控解读（代码注释登记）：分类名映射为内置→敏感-PII、自定义→敏感；
 * 字典规则 pattern 按逗号分隔关键字匹配（字典库管理完备化 seam-deferred）。</p>
 */
public final class SensitiveRecognizer {

    /** 内置规则命中 → 候选分类名/等级 */
    private static final String PII_NAME = "敏感-PII";
    private static final String PII_LEVEL = "PII";

    /** 自定义规则命中 → 候选分类名/等级 */
    private static final String SENSITIVE_NAME = "敏感";
    private static final String SENSITIVE_LEVEL = "SENSITIVE";

    /** 内置规则 → 列名/注释关键字（小写匹配） */
    private static final String[][] BUILTIN_KEYWORDS = {
            {"phone", "手机号", "手机", "电话", "mobile", "tel"},
            {"idcard", "身份证", "证件号", "id_card", "identity", "idno"},
            {"bankcard", "银行卡", "卡号", "bank_card", "card_no", "account_no"},
            {"email", "邮箱", "邮件", "email", "mail"},
    };

    private SensitiveRecognizer() {
    }

    /**
     * 识别一列：按启用规则序匹配，返回首个命中的候选（0 命中空列表）。
     *
     * @param column       待识别列
     * @param enabledRules 启用规则（空则仅内置规则）
     */
    public static List<RecognizedClassification> recognize(RecognizableColumn column, List<ClassRule> enabledRules) {
        List<RecognizedClassification> hits = new ArrayList<>();
        String name = column.getName() == null ? "" : column.getName().toLowerCase(Locale.ROOT);
        String comment = column.getComment() == null ? "" : column.getComment().toLowerCase(Locale.ROOT);
        String haystack = name + " " + comment;

        // 内置规则优先（内置命中即敏感-PII）
        for (String[] keywords : BUILTIN_KEYWORDS) {
            for (String keyword : keywords) {
                if (haystack.contains(keyword)) {
                    hits.add(RecognizedClassification.builder()
                            .name(PII_NAME).level(PII_LEVEL).build());
                    return hits;
                }
            }
        }

        // 自定义启用规则按序匹配（正则/列名/字典）
        if (enabledRules != null) {
            for (ClassRule rule : enabledRules) {
                if (Boolean.FALSE.equals(rule.getEnabled()) || rule.getType() == null) {
                    continue;
                }
                if (matches(rule, name, comment, haystack)) {
                    hits.add(RecognizedClassification.builder()
                            .name(SENSITIVE_NAME).level(SENSITIVE_LEVEL).build());
                    return hits;
                }
            }
        }
        return hits;
    }

    private static boolean matches(ClassRule rule, String name, String comment, String haystack) {
        String pattern = rule.getPattern();
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }
        switch (rule.getType()) {
            case REGEX:
                return matchesRegex(pattern, haystack);
            case COLUMN:
                return name.contains(pattern.trim().toLowerCase(Locale.ROOT));
            case DICTIONARY:
                for (String word : pattern.split(",")) {
                    String w = word.trim().toLowerCase(Locale.ROOT);
                    if (!w.isEmpty() && (name.contains(w) || comment.contains(w))) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private static boolean matchesRegex(String pattern, String haystack) {
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(haystack).find();
        } catch (RuntimeException e) {
            // 非法正则不阻断识别（登记：pattern 合法性校验为人工确认项）
            return false;
        }
    }
}
