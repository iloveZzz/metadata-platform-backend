package com.yss.datamiddle.dqinsight.client.dto;

import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 新建通道请求（冻结 OpenAPI ChannelCreate：name / type / formatType 必填；
 * scheduled-pull 时 schedule 必填；authToken writeOnly 加密存储，密文不回传）。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChannelCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通道名（重名 409 err.dq.channel.name-conflict） */
    private String name;

    /** 通道类型 */
    private ChannelType type;

    /** 拉取周期（cron；scheduled-pull 必填） */
    private String schedule;

    /** 格式类型 */
    private FormatType formatType;

    /** Token / AK-SK（writeOnly，加密存储，SB-09） */
    private String authToken;

    /** 目标数据域（缺省 = 全数据域） */
    private String domain;

    /** 创建后启用（默认 true） */
    private boolean enabled = true;
}
