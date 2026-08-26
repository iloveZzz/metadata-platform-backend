package com.yss.datamiddle.dqinsight.core.service.impl;

import com.yss.datamiddle.dqinsight.core.service.IngestionAppService;
import com.yss.datamiddle.dqinsight.domain.exception.BatchDuplicateException;
import com.yss.datamiddle.dqinsight.domain.exception.IngestValidationException;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelFetchPort;
import com.yss.datamiddle.dqinsight.domain.gateway.ChannelPullPort;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FetchResult;
import com.yss.datamiddle.dqinsight.domain.model.IngestionChannel;
import com.yss.datamiddle.dqinsight.domain.model.PullOutcome;
import com.yss.datamiddle.dqinsight.domain.util.IngestErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通道拉取执行（Application 编排，DQI-SLICE-04-WU2）。
 *
 * <p>编排：ChannelFetchPort 取数（Infrastructure）→ 成功内容复用切片 01 接入管线
 * （IngestionAppService.ingest，合同 seam）→ PullOutcome；重复批次（幂等去重 409）视为拉取完成
 * （数据已入库）。失败分类 format / auth / network + 脱敏信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPullExecutor implements ChannelPullPort {

    private final ChannelFetchPort channelFetchPort;
    private final IngestionAppService ingestionAppService;

    @Override
    public PullOutcome pull(IngestionChannel channel) {
        FetchResult fetched = channelFetchPort.fetch(channel);
        if (!fetched.isSuccess()) {
            return PullOutcome.failure(fetched.getErrorCategory(), fetched.getMessage());
        }
        try {
            ingestionAppService.ingest(fetched.getContent(), fetched.getContentType(),
                    String.valueOf(channel.getId()));
            return PullOutcome.success();
        } catch (BatchDuplicateException e) {
            // 幂等：批次已存在，数据已入库，视为拉取完成（不落拉取失败态）
            log.info("拉取批次幂等冲突（已入库，视为成功）: channelId={}, batchNo={}",
                    channel.getId(), e.getBatchNo());
            return PullOutcome.success();
        } catch (IngestValidationException e) {
            return PullOutcome.failure(e.getErrorCategory(),
                    IngestErrorMessages.summary(e.getErrorCategory(), e.getFieldErrors()));
        } catch (Exception e) {
            log.warn("拉取内容入库失败: channelId={}", channel.getId());
            return PullOutcome.failure(ErrorCategory.FORMAT, "拉取内容入库失败");
        }
    }
}
