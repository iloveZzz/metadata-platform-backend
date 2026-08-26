package com.yss.datasecurity.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 识别结果导入/手动添加冲突与去重策略枚举
 */
@Getter
@RequiredArgsConstructor
public enum DedupConflictStrategyEnum {
    OVERWRITE_ALL("OVERWRITE_ALL", "覆盖全部线上记录"),
    OVERWRITE_UNLOCKED("OVERWRITE_UNLOCKED", "仅覆盖未锁定记录"),
    RETAIN_EXISTING("RETAIN_EXISTING", "保留线上已有记录");

    private final String code;
    private final String description;

    public static DedupConflictStrategyEnum of(String code) {
        for (DedupConflictStrategyEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return OVERWRITE_ALL;
    }
}
