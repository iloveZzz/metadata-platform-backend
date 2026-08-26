package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;

import java.io.Serializable;

/**
 * 通道拉取取数结果（ChannelFetchPort 返回；成功 = 原始内容 + 内容类型，失败 = 分类 + 脱敏信息）。
 */
@Getter
public class FetchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;

    /** 取回原始内容（成功时非空） */
    private final String content;

    /** 内容类型（application/json / text/csv，按通道格式类型；成功时非空） */
    private final String contentType;

    /** 失败分类（format / auth / network，SB-04）；成功时为 null */
    private final ErrorCategory errorCategory;

    /** 脱敏失败信息；成功时为 null */
    private final String message;

    private FetchResult(boolean success, String content, String contentType,
            ErrorCategory errorCategory, String message) {
        this.success = success;
        this.content = content;
        this.contentType = contentType;
        this.errorCategory = errorCategory;
        this.message = message;
    }

    public static FetchResult success(String content, String contentType) {
        return new FetchResult(true, content, contentType, null, null);
    }

    public static FetchResult failure(ErrorCategory category, String message) {
        return new FetchResult(false, null, null, category, message);
    }
}
