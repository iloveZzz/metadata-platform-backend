package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelGateway;
import com.yss.datamiddle.dqinsight.domain.model.ChannelState;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通道仓储集成测试（DQI-SLICE-04-WU1/WU2）：save / findById / update / listAll /
 * 重名唯一约束（DuplicateKeyException → 409 name-conflict）/ 历史结果引用检查（409 in-use）/ delete。
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class ChannelGatewayTest {

    @Autowired
    private ChannelGateway channelGateway;

    @Autowired
    private DqBatchRepository dqBatchRepository;

    @Test
    void saveAndFindByIdRoundTripsStateMachineFields() {
        IngestionChannel channel = IngestionChannel.create("通道-1", ChannelType.SCHEDULED_PULL,
                "0 * * * * *", FormatType.GE, "交易域", true);
        channel.setAuthTokenEncrypted("enc-abc");
        channelGateway.save(channel);

        Optional<IngestionChannel> found = channelGateway.findById(channel.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("通道-1");
        assertThat(found.get().getType()).isEqualTo(ChannelType.SCHEDULED_PULL);
        assertThat(found.get().getState()).isEqualTo(ChannelState.ENABLED);
        assertThat(found.get().isAuthConfigured()).isTrue();
        assertThat(found.get().getAuthTokenEnc()).isEqualTo("enc-abc"); // 存储侧密文
    }

    @Test
    void duplicateNameViolatesUniqueConstraint() {
        IngestionChannel first = IngestionChannel.create("重名通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        IngestionChannel second = IngestionChannel.create("重名通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        channelGateway.save(first);

        assertThatThrownBy(() -> channelGateway.save(second)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void updatePersistsStateTransitions() {
        IngestionChannel channel = IngestionChannel.create("通道-2", ChannelType.SCHEDULED_PULL,
                "0 * * * * *", FormatType.GE, null, true);
        channelGateway.save(channel);

        channel.startPull();
        channelGateway.update(channel);
        assertThat(channelGateway.findById(channel.getId()).get().getState()).isEqualTo(ChannelState.PULLING);

        channel.markPullFailed(ErrorCategory.NETWORK, "网络超时");
        channelGateway.update(channel);
        IngestionChannel loaded = channelGateway.findById(channel.getId()).get();
        assertThat(loaded.getState()).isEqualTo(ChannelState.PULL_FAILED);
        assertThat(loaded.getErrorCategory()).isEqualTo(ErrorCategory.NETWORK);
        assertThat(loaded.getLastError()).isEqualTo("网络超时");
    }

    @Test
    void listAllExcludesNothingAndOrdersByCreatedAtDesc() {
        IngestionChannel a = IngestionChannel.create("列表-A", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        IngestionChannel b = IngestionChannel.create("列表-B", ChannelType.API_PUSH, null,
                FormatType.CSV, null, false);
        channelGateway.save(a);
        channelGateway.save(b);

        List<IngestionChannel> all = channelGateway.listAll();

        assertThat(all).extracting(IngestionChannel::getName).containsExactlyInAnyOrder("列表-A", "列表-B");
        assertThat(all).extracting(IngestionChannel::getState)
                .containsExactlyInAnyOrder(ChannelState.ENABLED, ChannelState.DISABLED);
    }

    @Test
    void listEnabledScheduledPullReturnsOnlyEnabledScheduledChannels() {
        IngestionChannel scheduled = IngestionChannel.create("调度-1", ChannelType.SCHEDULED_PULL,
                "0 * * * * *", FormatType.GE, null, true);
        IngestionChannel disabled = IngestionChannel.create("调度-2", ChannelType.SCHEDULED_PULL,
                "0 * * * * *", FormatType.GE, null, false);
        IngestionChannel push = IngestionChannel.create("推送-1", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        channelGateway.save(scheduled);
        channelGateway.save(disabled);
        channelGateway.save(push);

        List<IngestionChannel> due = channelGateway.listEnabledScheduledPull();

        assertThat(due).extracting(IngestionChannel::getName).containsExactly("调度-1");
    }

    @Test
    void hasHistoricalResultsDetectsBatchChannelReference() {
        IngestionChannel channel = IngestionChannel.create("引用通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        channelGateway.save(channel);
        assertThat(channelGateway.hasHistoricalResults(channel.getId())).isFalse();

        DqBatchPO batch = new DqBatchPO();
        batch.setId(999001L);
        batch.setBatchNo("ref-batch-1");
        batch.setSourceTool("great-expectations");
        batch.setFormatType("ge");
        batch.setStatus("ingested");
        batch.setLinkageStatus("linked");
        batch.setChannelId(String.valueOf(channel.getId()));
        batch.setReceivedAt(java.time.LocalDateTime.now());
        batch.setExecutionTime(java.time.LocalDateTime.now());
        batch.setRowCount(1);
        dqBatchRepository.insert(batch);

        assertThat(channelGateway.hasHistoricalResults(channel.getId())).isTrue();
    }

    @Test
    void deleteRemovesChannel() {
        IngestionChannel channel = IngestionChannel.create("删除通道", ChannelType.API_PUSH, null,
                FormatType.GE, null, true);
        channelGateway.save(channel);

        channelGateway.delete(channel.getId());

        assertThat(channelGateway.findById(channel.getId())).isEmpty();
        assertThat(channelGateway.listAll()).isEmpty();
    }
}
