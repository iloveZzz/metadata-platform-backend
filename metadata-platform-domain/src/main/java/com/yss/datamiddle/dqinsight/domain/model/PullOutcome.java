package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 通道拉取结果（ChannelPullPort 返回；成功 = 已复用切片 01 接入管线入库，失败 = 分类 + 脱敏信息）。
 */
@Getter
public class PullOutcome implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;

    /** 失败分类（format / auth / network，SB-04）；成功时为 null */
    private final ErrorCategory errorCategory;

    /** 脱敏错误信息；成功时为 null */
    private final String message;

    private PullOutcome(boolean success, ErrorCategory errorCategory, String message) {
        this.success = success;
        this.errorCategory = errorCategory;
        this.message = message;
    }

    public static PullOutcome success() {
        return new PullOutcome(true, null, null);
    }

    public static PullOutcome failure(ErrorCategory category, String message) {
        return new PullOutcome(false, category, message);
    }
}
