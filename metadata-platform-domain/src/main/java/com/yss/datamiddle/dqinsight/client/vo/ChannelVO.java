package com.yss.datamiddle.dqinsight.client.vo;

import com.yss.datamiddle.dqinsight.domain.model.ChannelState;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 接入通道（冻结 OpenAPI Channel；认证配置密文不回传，仅 authConfigured，C19）。
 */
@Getter
@Setter
@NoArgsConstructor
public class ChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通道 ID */
    private String id;

    /** 通道名 */
    private String name;

    /** 通道类型（api-push / scheduled-pull） */
    private ChannelType type;

    /** 拉取周期（仅 scheduled-pull） */
    private String schedule;

    /** 格式类型（GE / 通用 CSV / 通用 API） */
    private FormatType formatType;

    /** 认证配置是否已设置（密文不返回） */
    private boolean authConfigured;

    /** 状态（enabled / disabled / pulling / pull-failed） */
    private ChannelState state;

    /** 上次拉取时间（ISO 8601；未拉取过为 null） */
    private String lastPullAt;

    /** 错误信息（脱敏；拉取失败展示） */
    private String lastError;

    /** 错误分类（format / auth / network） */
    private ErrorCategory errorCategory;

    /** 目标数据域（缺省 = 全数据域） */
    private String domain;
}
