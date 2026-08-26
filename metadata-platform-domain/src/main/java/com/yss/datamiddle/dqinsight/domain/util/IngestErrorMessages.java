package com.yss.datamiddle.dqinsight.domain.util;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 接入错误信息汇总（脱敏）。
 *
 * <p>只汇总错误分类与字段路径（CSV 行号 row:N），不回显请求内容 / 凭证（C19）。</p>
 */
public final class IngestErrorMessages {

    private static final int MAX_FIELD_PATHS_IN_SUMMARY = 3;

    private IngestErrorMessages() {
    }

    public static String summary(ErrorCategory category, List<FieldErrorItem> fieldErrors) {
        StringBuilder sb = new StringBuilder();
        sb.append("接入解析失败，错误分类 ")
                .append(category == null ? ErrorCategory.FORMAT.getCode() : category.getCode());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            List<String> paths = fieldErrors.stream()
                    .limit(MAX_FIELD_PATHS_IN_SUMMARY)
                    .map(FieldErrorItem::getField)
                    .collect(Collectors.toList());
            sb.append('：').append(String.join(", ", paths));
            if (fieldErrors.size() > MAX_FIELD_PATHS_IN_SUMMARY) {
                sb.append(" 等 ").append(fieldErrors.size()).append(" 处错误");
            }
        }
        return sb.toString();
    }
}
