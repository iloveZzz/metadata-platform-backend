package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.cloud.dto.page.PageQuery;
import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.client.vo.PendingLinkageVO;
import com.yss.datamiddle.dqinsight.domain.gateway.LinkageGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.LinkageMatchMode;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.repository.DqAssetLinkageRepository;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.infrastructure.convertor.DqAssetLinkageConvertor;
import com.yss.datamiddle.dqinsight.repository.entity.DqAssetLinkagePO;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资产关联治理仓储集成测试（DQI-SLICE-04-WU3）：pending 队列分页（join 批次字段）/
 * 空队列空分页 / findById / 人工映射 save（保存资产快照 + state=linked）。
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class LinkageGatewayTest {

    @Autowired
    private LinkageGateway linkageGateway;

    @Autowired
    private DqAssetLinkageRepository dqAssetLinkageRepository;

    @Autowired
    private DqBatchRepository dqBatchRepository;

    @Test
    void listPendingJoinsBatchFields() {
        Long linkageId = seedPendingLinkage("asset-missing-1", "batch-pending-1");

        PageQuery query = new PageQuery();
        query.setPageIndex(1);
        query.setPageSize(20);
        List<PendingLinkageVO> pending = linkageGateway.listPending(query);

        assertThat(pending).hasSize(1);
        PendingLinkageVO vo = pending.get(0);
        assertThat(vo.getId()).isEqualTo(String.valueOf(linkageId));
        assertThat(vo.getAssetId()).isEqualTo("asset-missing-1");
        assertThat(vo.getBatchNo()).isEqualTo("batch-pending-1");
        assertThat(vo.getSourceTool()).isEqualTo(SourceTool.GREAT_EXPECTATIONS);
        assertThat(vo.getRowCount()).isEqualTo(3);
        assertThat(vo.getReceivedAt()).isNotBlank();
    }

    @Test
    void listPendingExcludesLinkedRowsAndEmptyQueueIsEmptyPage() {
        Long linkageId = seedPendingLinkage("asset-missing-2", "batch-pending-2");
        DqAssetLinkagePO po = dqAssetLinkageRepository.selectById(linkageId);
        po.setState(LinkageState.LINKED.getCode());
        dqAssetLinkageRepository.updateById(po);

        PageQuery query = new PageQuery();
        query.setPageIndex(1);
        query.setPageSize(20);
        assertThat(linkageGateway.listPending(query)).isEmpty();
        assertThat(query.getTempTotalCount()).isZero();
    }

    @Test
    void findByIdLoadsDomainAggregate() {
        Long linkageId = seedPendingLinkage("asset-missing-3", "batch-pending-3");

        AssetLinkage linkage = linkageGateway.findById(linkageId);

        assertThat(linkage).isNotNull();
        assertThat(linkage.getState()).isEqualTo(LinkageState.PENDING);
        assertThat(linkage.getSourceAssetId()).isEqualTo("asset-missing-3");
        assertThat(linkageGateway.findById(999999L)).isNull();
    }

    @Test
    void savePersistsManualMappingSnapshot() {
        Long linkageId = seedPendingLinkage("asset-missing-4", "batch-pending-4");
        AssetLinkage linkage = linkageGateway.findById(linkageId);

        linkage.setResolvedAssetId("asset-resolved-4");
        linkage.setAssetName("解析表");
        linkage.setDomain("交易域");
        linkage.setAssetType("table");
        linkage.setMatchMode(LinkageMatchMode.MANUAL);
        linkage.setState(LinkageState.LINKED);
        linkage.setMappedAt(Instant.now());
        linkage.setMappedBy("admin");
        linkageGateway.save(linkage);

        AssetLinkage loaded = linkageGateway.findById(linkageId);
        assertThat(loaded.getState()).isEqualTo(LinkageState.LINKED);
        assertThat(loaded.getResolvedAssetId()).isEqualTo("asset-resolved-4");
        assertThat(loaded.getAssetName()).isEqualTo("解析表");
        assertThat(loaded.getDomain()).isEqualTo("交易域");
        assertThat(loaded.getMatchMode()).isEqualTo(LinkageMatchMode.MANUAL);
        assertThat(loaded.getMappedBy()).isEqualTo("admin");
    }

    private Long seedPendingLinkage(String assetId, String batchNo) {
        DQResultBatch batch = DQResultBatch.createIngested(batchNo, SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, "ch-1", Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());
        DqBatchPO batchPo = new DqBatchPO();
        batchPo.setBatchNo(batchNo);
        batchPo.setSourceTool("great-expectations");
        batchPo.setFormatType("ge");
        batchPo.setStatus("ingested");
        batchPo.setLinkageStatus("pending");
        batchPo.setReceivedAt(LocalDateTime.now());
        batchPo.setExecutionTime(LocalDateTime.now());
        batchPo.setRowCount(3);
        dqBatchRepository.insert(batchPo);

        AssetLinkage linkage = AssetLinkage.pending(batch, assetId);
        DqAssetLinkagePO po = Mappers.getMapper(DqAssetLinkageConvertor.class).toPO(linkage);
        po.setBatchId(batchPo.getId());
        dqAssetLinkageRepository.insert(po);
        return po.getId();
    }
}
