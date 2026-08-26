package com.yss.datamiddle.aicontextlayer.repository.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间换算工具（领域 Instant ↔ 存储 datetime 墙钟时间）。
 *
 * <p>与既有 Convertor 口径一致（D1 评审点 6）：统一按 Asia/Shanghai
 * （bootstrap 数据源 serverTimezone=Asia/Shanghai）换算，保证同一时刻读写往返一致。</p>
 */
public final class TimeSupport {

    public static final ZoneId TIME_ZONE = ZoneId.of("Asia/Shanghai");

    private TimeSupport() {
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, TIME_ZONE);
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(TIME_ZONE).toInstant();
    }
}
