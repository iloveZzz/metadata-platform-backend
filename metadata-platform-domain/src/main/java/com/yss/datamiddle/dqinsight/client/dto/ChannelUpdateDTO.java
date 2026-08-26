package com.yss.datamiddle.dqinsight.client.dto;

import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通道更新请求（冻结 OpenAPI ChannelUpdate：部分更新，至少携带一个字段（服务端校验 422）；
 * enabled 用于启停（停用需二次确认）；拉取中更新 409 err.dq.channel.busy）。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChannelUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通道名 */
    private String name;

    /** 拉取周期（cron） */
    private String schedule;

    /** 格式类型 */
    private FormatType formatType;

    /** Token / AK-SK（writeOnly；更新时重设认证） */
    private String authToken;

    /** 目标数据域 */
    private String domain;

    /** 启停（enabled = true 启用 / false 停用） */
    private Boolean enabled;

    /**
     * 是否至少携带一个字段（契约 minProperties 语义，服务端校验）。
     */
    public boolean isEmptyUpdate() {
        return name == null && schedule == null && formatType == null
                && authToken == null && domain == null && enabled == null;
    }
}
