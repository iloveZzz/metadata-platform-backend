package com.yss.datamiddle.dqinsight.domain.model;

import com.yss.datamiddle.dqinsight.domain.exception.ChannelBusyException;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 接入通道聚合（IngestionChannel，DQI-005 / SB-06，数据架构 §3/§5）。
 *
 * <p>状态机（C25）：启用 / 停用 ↔ 拉取中 ↔ 拉取失败。领域规则：
 * 拉取中不可更新配置 / 启停（409 busy）；重试仅从 拉取失败 / 启用 进入 拉取中，拉取中重复触发与
 * 停用通道重试均 409 busy（幂等）；拉取成功回到 启用（记录上次拉取时间），失败进入 拉取失败
 * （错误分类 format / auth / network，C18）。认证凭证加密存储（auth_token_enc，密文不回传，
 * 仅 authConfigured，C19）。</p>
 */
@Getter
@Setter
public class IngestionChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（保存后由持久化分配） */
    private Long id;

    /** 通道名（未删除唯一，重名 409 name-conflict） */
    private String name;

    /** 通道类型（API 推送 / 定时拉取） */
    private ChannelType type;

    /** 拉取周期（cron 表达式，仅 scheduled-pull 生效） */
    private String schedule;

    /** 格式类型（GE / 通用 CSV / 通用 API） */
    private FormatType formatType;

    /** 认证凭证密文（成熟加密库，密文不回传） */
    private String authTokenEnc;

    /** 认证是否已配置（布尔，唯一对外暴露形态） */
    private boolean authConfigured;

    /** 目标数据域（缺省 = 全数据域） */
    private String domain;

    /** 通道状态（启用 / 停用 / 拉取中 / 拉取失败） */
    private ChannelState state;

    /** 上次拉取时间 */
    private Instant lastPullAt;

    /** 错误信息（拉取失败展示，脱敏） */
    private String lastError;

    /** 错误分类（format / auth / network，SB-04） */
    private ErrorCategory errorCategory;

    /** 软删除位（MVP 无历史结果时物理删除，保留字段防御） */
    private Instant deletedAt;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间（乐观并发版本位） */
    private Instant updatedAt;

    private IngestionChannel() {
    }

    /**
     * 新建通道（默认启用；停用需在创建后二次确认场景走 toggle）。
     */
    public static IngestionChannel create(String name, ChannelType type, String schedule,
            FormatType formatType, String domain, boolean enabled) {
        IngestionChannel channel = new IngestionChannel();
        channel.name = name;
        channel.type = type;
        channel.schedule = schedule;
        channel.formatType = formatType;
        channel.domain = domain;
        channel.state = enabled ? ChannelState.ENABLED : ChannelState.DISABLED;
        channel.authConfigured = false;
        channel.createdAt = Instant.now();
        channel.updatedAt = channel.createdAt;
        return channel;
    }

    /**
     * 持久化 / 映射专用构造（仅 MapStruct 反向映射 toDomain 使用；业务创建必须使用 create）。
     */
    public static IngestionChannel forPersistenceLoad() {
        return new IngestionChannel();
    }

    /**
     * 启停（停用需二次确认；拉取中不可启停，409 busy）。
     */
    public void toggle(boolean enable) {
        if (state == ChannelState.PULLING) {
            throw new ChannelBusyException(id, "通道正在拉取中，不可启停");
        }
        state = enable ? ChannelState.ENABLED : ChannelState.DISABLED;
        updatedAt = Instant.now();
    }

    /**
     * 进入拉取中（定时触发 / 手动重试；拉取中重复触发与停用通道重试均 409 busy，幂等）。
     */
    public void startPull() {
        if (state == ChannelState.PULLING) {
            throw new ChannelBusyException(id, "通道正在拉取中，请勿重复触发");
        }
        if (state == ChannelState.DISABLED) {
            throw new ChannelBusyException(id, "通道已停用，无法拉取");
        }
        state = ChannelState.PULLING;
        updatedAt = Instant.now();
    }

    /**
     * 拉取成功：回到 启用，记录上次拉取时间，清空错误字段。
     */
    public void markPullSucceeded(Instant pulledAt) {
        state = ChannelState.ENABLED;
        lastPullAt = pulledAt;
        lastError = null;
        errorCategory = null;
        updatedAt = Instant.now();
    }

    /**
     * 拉取失败：进入 拉取失败，记录错误分类与脱敏错误信息。
     */
    public void markPullFailed(ErrorCategory category, String message) {
        state = ChannelState.PULL_FAILED;
        errorCategory = category;
        lastError = message;
        updatedAt = Instant.now();
    }

    /**
     * 部分更新配置（至少一个字段由调用方校验；拉取中不可更新，409 busy）。
     */
    public void applyPartialUpdate(String newName, String newSchedule, FormatType newFormatType,
            String newDomain) {
        if (state == ChannelState.PULLING) {
            throw new ChannelBusyException(id, "通道正在拉取中，不可更新配置");
        }
        if (newName != null && !newName.trim().isEmpty()) {
            name = newName.trim();
        }
        if (newSchedule != null) {
            schedule = newSchedule;
        }
        if (newFormatType != null) {
            formatType = newFormatType;
        }
        if (newDomain != null) {
            domain = newDomain;
        }
        updatedAt = Instant.now();
    }

    /**
     * 设置认证凭证密文（更新时调用方先加密；密文不回传，仅 authConfigured）。
     */
    public void setAuthTokenEncrypted(String encryptedToken) {
        this.authTokenEnc = encryptedToken;
        this.authConfigured = encryptedToken != null && !encryptedToken.isEmpty();
        updatedAt = Instant.now();
    }

    /**
     * 是否定时拉取通道。
     */
    public boolean isScheduledPull() {
        return type == ChannelType.SCHEDULED_PULL;
    }
}
