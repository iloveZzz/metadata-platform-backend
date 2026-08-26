package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.cloud.dto.result.PageResult;
import com.yss.datamiddle.dqinsight.client.dto.ChannelCreateDTO;
import com.yss.datamiddle.dqinsight.client.dto.ChannelUpdateDTO;
import com.yss.datamiddle.dqinsight.client.vo.ChannelVO;
import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.core.service.ChannelAppService;
import com.yss.datamiddle.dqinsight.core.service.convertor.ChannelConvertor;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelBusyException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelInUseException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelNameConflictException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelNotFoundException;
import com.yss.datamiddle.dqinsight.domain.exception.ChannelValidationException;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelPullPort;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelTokenEncryptor;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.AuditResult;
import com.yss.datamiddle.dqinsight.domain.model.ChannelType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.domain.model.PullOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通道管理用例编排实现（Application 只编排，C10；单聚合事务边界）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAppServiceImpl implements ChannelAppService {

    private static final String DEFAULT_OPERATOR = "system";

    private final ChannelGateway channelGateway;
    private final ChannelTokenEncryptor channelTokenEncryptor;
    private final ChannelPullPort channelPullPort;
    private final AuditLogGateway auditLogGateway;
    private final ChannelConvertor channelConvertor;

    @Override
    public List<ChannelVO> listChannels() {
        return channelGateway.listAll().stream()
                .map(channelConvertor::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChannelVO getChannel(Long id) {
        return channelConvertor.toVO(requireChannel(id));
    }

    @Override
    @Transactional
    public ChannelVO createChannel(ChannelCreateDTO dto, String operator) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ChannelValidationException(DqErrorCodes.CHANNEL_NAME_CONFLICT, "通道名必填",
                    Collections.singletonList(FieldErrorItem.of("name", "err.dq.channel.name-required", "通道名必填")));
        }
        if (dto.getType() == null || dto.getFormatType() == null) {
            throw new ChannelValidationException(DqErrorCodes.FORMAT_INVALID, "通道类型与格式类型必填",
                    Collections.singletonList(FieldErrorItem.of("type", DqErrorCodes.FORMAT_INVALID, "通道类型 / 格式类型必填")));
        }
        if (dto.getType() == ChannelType.SCHEDULED_PULL
                && (dto.getSchedule() == null || dto.getSchedule().trim().isEmpty())) {
            throw new ChannelValidationException(DqErrorCodes.FORMAT_INVALID, "定时拉取通道必须配置拉取周期",
                    Collections.singletonList(FieldErrorItem.of("schedule", DqErrorCodes.FORMAT_INVALID, "定时拉取通道必须配置拉取周期")));
        }

        IngestionChannel channel = IngestionChannel.create(dto.getName().trim(), dto.getType(),
                dto.getSchedule(), dto.getFormatType(), dto.getDomain(), dto.isEnabled());
        applyTokenIfPresent(channel, dto.getAuthToken());
        try {
            channelGateway.save(channel);
        } catch (DuplicateKeyException e) {
            log.warn("通道重名冲突（唯一约束兜底）: name={}", channel.getName());
            throw new ChannelNameConflictException(channel.getName());
        }
        auditLogGateway.record(AuditLogEntry.channelConfig(operatorOf(operator), channel.getName(),
                "type=" + channel.getType().getCode() + ", formatType=" + channel.getFormatType().getCode()
                        + ", authConfigured=" + channel.isAuthConfigured()));
        return channelConvertor.toVO(channel);
    }

    @Override
    @Transactional
    public ChannelVO updateChannel(Long id, ChannelUpdateDTO dto, String operator) {
        if (dto.isEmptyUpdate()) {
            throw new ChannelValidationException(DqErrorCodes.FORMAT_INVALID, "更新请求至少携带一个字段",
                    Collections.singletonList(FieldErrorItem.of("body", DqErrorCodes.FORMAT_INVALID, "至少携带一个字段")));
        }
        IngestionChannel channel = requireChannel(id);

        boolean toggled = false;
        if (dto.getEnabled() != null) {
            channel.toggle(dto.getEnabled());
            toggled = true;
        }
        channel.applyPartialUpdate(dto.getName(), dto.getSchedule(), dto.getFormatType(), dto.getDomain());
        applyTokenIfPresent(channel, dto.getAuthToken());
        channelGateway.update(channel);

        if (toggled) {
            auditLogGateway.record(AuditLogEntry.channelToggle(operatorOf(operator), channel.getName(),
                    "enabled=" + channel.getState().getCode()));
        }
        boolean configChanged = dto.getName() != null || dto.getSchedule() != null
                || dto.getFormatType() != null || dto.getDomain() != null || dto.getAuthToken() != null;
        if (configChanged) {
            auditLogGateway.record(AuditLogEntry.channelConfig(operatorOf(operator), channel.getName(),
                    "authConfigured=" + channel.isAuthConfigured()));
        }
        return channelConvertor.toVO(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long id, String operator) {
        IngestionChannel channel = requireChannel(id);
        if (channelGateway.hasHistoricalResults(id)) {
            throw new ChannelInUseException(id);
        }
        channelGateway.delete(id);
        auditLogGateway.record(AuditLogEntry.channelConfig(operatorOf(operator), channel.getName(), "删除通道"));
    }

    @Override
    @Transactional
    public ChannelVO retryPull(Long id, String operator) {
        IngestionChannel channel = requireChannel(id);
        channel.startPull();
        channelGateway.update(channel);
        PullOutcome outcome = channelPullPort.pull(channel);
        return finalizePull(channel, outcome, operatorOf(operator));
    }

    @Override
    @Transactional
    public void runScheduledPull(Long channelId) {
        Optional<IngestionChannel> found = channelGateway.findById(channelId);
        if (!found.isPresent()) {
            return;
        }
        IngestionChannel channel = found.get();
        try {
            channel.startPull();
        } catch (ChannelBusyException e) {
            log.info("定时拉取跳过（拉取中 / 已停用）: channelId={}", channelId);
            return;
        }
        channelGateway.update(channel);
        PullOutcome outcome = channelPullPort.pull(channel);
        finalizePull(channel, outcome, DEFAULT_OPERATOR);
    }

    private ChannelVO finalizePull(IngestionChannel channel, PullOutcome outcome, String operator) {
        if (outcome.isSuccess()) {
            channel.markPullSucceeded(Instant.now());
            auditLogGateway.record(AuditLogEntry.channelRetry(operator, channel.getName(),
                    "拉取成功", AuditResult.SUCCESS));
        } else {
            channel.markPullFailed(outcome.getErrorCategory(), outcome.getMessage());
            auditLogGateway.record(AuditLogEntry.channelRetry(operator, channel.getName(),
                    "拉取失败：" + outcome.getMessage(), AuditResult.FAILURE));
        }
        channelGateway.update(channel);
        return channelConvertor.toVO(channel);
    }

    private IngestionChannel requireChannel(Long id) {
        return channelGateway.findById(id)
                .orElseThrow(() -> new ChannelNotFoundException(id));
    }

    private void applyTokenIfPresent(IngestionChannel channel, String authToken) {
        if (authToken != null && !authToken.trim().isEmpty()) {
            channel.setAuthTokenEncrypted(channelTokenEncryptor.encrypt(authToken.trim()));
        }
    }

    private static String operatorOf(String operator) {
        return operator == null || operator.isEmpty() ? DEFAULT_OPERATOR : operator;
    }
}
