package com.yss.datamiddle.dqinsight.domain.model;

import com.yss.datamiddle.dqinsight.domain.exception.ChannelBusyException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通道状态机领域测试（DQI-SLICE-04-WU1，C25）。
 *
 * <p>状态流转：启用 / 停用 ↔ 拉取中 ↔ 拉取失败；重试幂等（拉取中重复触发 / 停用通道重试 409 busy）；
 * 拉取中不可更新配置与启停；拉取成功 / 失败分类记录；认证凭证密文不回传（authConfigured 布尔）。</p>
 */
class ChannelStateTest {

    private static IngestionChannel channel(boolean enabled) {
        return IngestionChannel.create("通道A", ChannelType.SCHEDULED_PULL, "0 * * * * *",
                FormatType.GE, "交易域", enabled);
    }

    @Test
    void createDefaultsToRequestedEnabledState() {
        assertThat(channel(true).getState()).isEqualTo(ChannelState.ENABLED);
        assertThat(channel(false).getState()).isEqualTo(ChannelState.DISABLED);
        assertThat(channel(true).isScheduledPull()).isTrue();
    }

    @Test
    void toggleFlipsEnabledAndDisabled() {
        IngestionChannel c = channel(true);
        c.toggle(false);
        assertThat(c.getState()).isEqualTo(ChannelState.DISABLED);
        c.toggle(true);
        assertThat(c.getState()).isEqualTo(ChannelState.ENABLED);
    }

    @Test
    void toggleWhilePullingRejectedAsBusy() {
        IngestionChannel c = channel(true);
        c.startPull();
        assertThat(c.getState()).isEqualTo(ChannelState.PULLING);

        assertThatThrownBy(() -> c.toggle(false)).isInstanceOf(ChannelBusyException.class);
    }

    @Test
    void startPullFromEnabledOrPullFailedEntersPulling() {
        IngestionChannel c = channel(true);
        c.startPull();
        assertThat(c.getState()).isEqualTo(ChannelState.PULLING);

        c.markPullFailed(ErrorCategory.NETWORK, "网络超时");
        assertThat(c.getState()).isEqualTo(ChannelState.PULL_FAILED);
        c.startPull(); // 重试
        assertThat(c.getState()).isEqualTo(ChannelState.PULLING);
    }

    @Test
    void startPullWhilePullingRejectedIdempotent() {
        IngestionChannel c = channel(true);
        c.startPull();
        assertThatThrownBy(c::startPull).isInstanceOf(ChannelBusyException.class);
    }

    @Test
    void startPullWhileDisabledRejected() {
        IngestionChannel c = channel(false);
        assertThatThrownBy(c::startPull).isInstanceOf(ChannelBusyException.class);
    }

    @Test
    void pullSucceededReturnsToEnabledAndClearsErrors() {
        IngestionChannel c = channel(true);
        c.startPull();
        c.markPullFailed(ErrorCategory.AUTH, "认证失败");
        c.startPull();
        Instant pulledAt = Instant.now();
        c.markPullSucceeded(pulledAt);

        assertThat(c.getState()).isEqualTo(ChannelState.ENABLED);
        assertThat(c.getLastPullAt()).isEqualTo(pulledAt);
        assertThat(c.getLastError()).isNull();
        assertThat(c.getErrorCategory()).isNull();
    }

    @Test
    void pullFailedRecordsCategoryAndDesensitizedMessage() {
        IngestionChannel c = channel(true);
        c.startPull();
        c.markPullFailed(ErrorCategory.FORMAT, "格式不支持");

        assertThat(c.getState()).isEqualTo(ChannelState.PULL_FAILED);
        assertThat(c.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(c.getLastError()).isEqualTo("格式不支持");
    }

    @Test
    void partialUpdateWhilePullingRejectedAsBusy() {
        IngestionChannel c = channel(true);
        c.startPull();
        assertThatThrownBy(() -> c.applyPartialUpdate("新名", null, null, null))
                .isInstanceOf(ChannelBusyException.class);
    }

    @Test
    void partialUpdateAppliesOnlyNonNullFields() {
        IngestionChannel c = channel(true);
        c.applyPartialUpdate("新名", null, FormatType.CSV, null);

        assertThat(c.getName()).isEqualTo("新名");
        assertThat(c.getFormatType()).isEqualTo(FormatType.CSV);
        assertThat(c.getSchedule()).isEqualTo("0 * * * * *"); // 未更新字段保持原值
    }

    @Test
    void authTokenEncryptedExposedOnlyAsConfiguredFlag() {
        IngestionChannel c = channel(true);
        assertThat(c.isAuthConfigured()).isFalse();
        assertThat(c.getAuthTokenEnc()).isNull();

        c.setAuthTokenEncrypted("ciphertext-abc");
        assertThat(c.isAuthConfigured()).isTrue();
        assertThat(c.getAuthTokenEnc()).isEqualTo("ciphertext-abc"); // 存储侧密文
    }
}
