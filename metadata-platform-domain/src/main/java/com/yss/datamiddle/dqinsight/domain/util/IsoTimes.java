package com.yss.datamiddle.dqinsight.domain.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * ISO 8601 时间工具（冻结契约 executionTime / receivedAt / validUntil 均使用 date-time 格式）。
 */
public final class IsoTimes {

    private IsoTimes() {
    }

    /**
     * 解析 ISO 8601 时间（接受带时区偏移或 Z 结尾）；失败返回 null。
     */
    public static Instant parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 格式化 ISO 8601 时间（带系统时区偏移）。
     */
    public static String format(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(OffsetDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
    }
}
