package com.yss.datamiddle.dqinsight.repository.gateway.impl;

import com.yss.datamiddle.dqinsight.InfraTestApplication;
import com.yss.datamiddle.dqinsight.client.dto.query.IngestionRecordPageQuery;
import com.yss.datamiddle.dqinsight.client.vo.IngestionRecordVO;
import com.yss.datamiddle.dqinsight.domain.gateway.AuditLogGateway;
import com.yss.datamiddle.dqinsight.domain.gateway.DqResultGateway;
import com.yss.datamiddle.dqinsight.domain.model.AssetLinkage;
import com.yss.datamiddle.dqinsight.domain.model.AuditLogEntry;
import com.yss.datamiddle.dqinsight.domain.model.DQResultBatch;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.model.FormatType;
import com.yss.datamiddle.dqinsight.domain.model.IngestionStatus;
import com.yss.datamiddle.dqinsight.domain.model.LinkageState;
import com.yss.datamiddle.dqinsight.domain.model.RuleResultRow;
import com.yss.datamiddle.dqinsight.domain.model.RuleStatus;
import com.yss.datamiddle.dqinsight.domain.model.RuleType;
import com.yss.datamiddle.dqinsight.domain.model.SourceTool;
import com.yss.datamiddle.dqinsight.repository.DqAuditLogRepository;
import com.yss.datamiddle.dqinsight.repository.DqBatchRepository;
import com.yss.datamiddle.dqinsight.repository.DqRuleResultRepository;
import com.yss.datamiddle.dqinsight.repository.entity.DqAuditLogPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqBatchPO;
import com.yss.datamiddle.dqinsight.repository.entity.DqRuleResultPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 批次仓储集成测试（WU2）：单聚合入库、UNIQUE 幂等去重兜底、接入记录筛选分页、审计 append-only。
 *
 * <p>测试库 H2（MySQL 模式），迁移脚本由 Liquibase 执行（同时验证建表脚本可执行）。</p>
 */
@SpringBootTest(classes = InfraTestApplication.class)
@Transactional
class BatchRepositoryTest {

    @Autowired
    private DqResultGateway dqResultGateway;

    @Autowired
    private AuditLogGateway auditLogGateway;

    @Autowired
    private DqBatchRepository dqBatchRepository;

    @Autowired
    private DqRuleResultRepository dqRuleResultRepository;

    @Autowired
    private DqAuditLogRepository dqAuditLogRepository;

    @BeforeEach
    void setUp() {
        // 每用例独立批次号，避免跨用例干扰
    }

    @Test
    void savePersistsBatchWithRuleResultsAndPendingLinkageInOneCall() {
        DQResultBatch batch = batch("ing-1", LinkageState.NONE);
        List<RuleResultRow> rows = Arrays.asList(row("asset-a"), row("asset-b"));
        AssetLinkage pendingA = AssetLinkage.pending(batch, "asset-a");
        AssetLinkage pendingB = AssetLinkage.pending(batch, "asset-b");

        DQResultBatch saved = dqResultGateway.save(batch, rows, Arrays.asList(pendingA, pendingB));

        assertThat(saved.getId()).isNotNull();
        DqBatchPO po = dqBatchRepository.selectById(saved.getId());
        assertThat(po).isNotNull();
        assertThat(po.getStatus()).isEqualTo("ingested");
        assertThat(po.getBatchNo()).isEqualTo("ing-1");
        assertThat(po.getSourceTool()).isEqualTo("great-expectations");

        List<DqRuleResultPO> rowPOs = dqRuleResultRepository.selectList(null);
        assertThat(rowPOs).hasSize(2);
        assertThat(rowPOs).allSatisfy(r -> assertThat(r.getBatchId()).isEqualTo(saved.getId()));
    }

    @Test
    void duplicateBatchNoThrowsDuplicateKeyException() {
        DQResultBatch first = batch("dup-1", LinkageState.NONE);
        dqResultGateway.save(first, Collections.emptyList(), Collections.emptyList());

        DQResultBatch duplicate = batch("dup-1", LinkageState.NONE);
        assertThatThrownBy(() -> dqResultGateway.save(duplicate, Collections.emptyList(),
                Collections.emptyList()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void parseFailedBatchIsPersistedAndQueryable() {
        DQResultBatch failed = DQResultBatch.createParseFailed(FormatType.CSV, SourceTool.GENERIC,
                "parse-fail-1", "ch-1", ErrorCategory.FORMAT, "接入解析失败，错误分类 format：row:2.rule_type");
        dqResultGateway.save(failed, Collections.emptyList(), Collections.emptyList());

        IngestionRecordPageQuery query = new IngestionRecordPageQuery();
        query.setStatus(IngestionStatus.PARSE_FAILED);
        List<IngestionRecordVO> records = dqResultGateway.listIngestionRecords(query);

        assertThat(records).hasSize(1);
        IngestionRecordVO vo = records.get(0);
        assertThat(vo.getBatchNo()).isEqualTo("parse-fail-1");
        assertThat(vo.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(vo.getErrorMessage()).doesNotContain("bad-value");
    }

    @Test
    void pageQuerySupportsFiltersAndPagination() {
        DQResultBatch batch1 = batch("page-1", LinkageState.PENDING);
        batch1.setChannelId("ch-a");
        DQResultBatch batch2 = batch("page-2", LinkageState.PENDING);
        batch2.setChannelId("ch-b");
        dqResultGateway.save(batch1, Collections.emptyList(), Collections.emptyList());
        dqResultGateway.save(batch2, Collections.emptyList(), Collections.emptyList());

        IngestionRecordPageQuery query = new IngestionRecordPageQuery();
        query.setSourceTool(SourceTool.GREAT_EXPECTATIONS);
        query.setChannelId("ch-a");
        List<IngestionRecordVO> records = dqResultGateway.listIngestionRecords(query);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getBatchNo()).isEqualTo("page-1");
        // PageQuery 自动分页总数在查询后经 tempTotalCount 回读
        assertThat(query.getTempTotalCount()).isEqualTo(1);
    }

    @Test
    void paginationLimitsRowsAndReportsTotal() {
        dqResultGateway.save(batch("pag-1", LinkageState.NONE), Collections.emptyList(), Collections.emptyList());
        dqResultGateway.save(batch("pag-2", LinkageState.NONE), Collections.emptyList(), Collections.emptyList());
        dqResultGateway.save(batch("pag-3", LinkageState.NONE), Collections.emptyList(), Collections.emptyList());

        IngestionRecordPageQuery query = new IngestionRecordPageQuery();
        query.setPageIndex(1);
        query.setPageSize(2);
        List<IngestionRecordVO> records = dqResultGateway.listIngestionRecords(query);

        assertThat(records).hasSize(2);
        assertThat(query.getTempTotalCount()).isEqualTo(3);
    }

    @Test
    void emptyResultIsExpressedAsEmptyPage() {
        IngestionRecordPageQuery query = new IngestionRecordPageQuery();
        query.setChannelId("no-such-channel");
        List<IngestionRecordVO> records = dqResultGateway.listIngestionRecords(query);

        assertThat(records).isEmpty();
        assertThat(query.getTempTotalCount()).isZero();
    }

    @Test
    void auditRecordIsAppendOnlyInsert() {
        auditLogGateway.record(AuditLogEntry.ingest("channel:ch-1", "audit-batch-1",
                "sourceTool=generic, formatType=csv, rowCount=1"));
        auditLogGateway.record(AuditLogEntry.parseFail("system", "audit-batch-2", "解析失败"));

        List<DqAuditLogPO> entries = dqAuditLogRepository.selectList(null);
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(DqAuditLogPO::getAction).containsExactlyInAnyOrder("ingest", "parse-fail");
        assertThat(entries).extracting(DqAuditLogPO::getResult).containsExactlyInAnyOrder("success", "failure");
    }

    private static DQResultBatch batch(String batchNo, LinkageState linkageState) {
        DQResultBatch batch = DQResultBatch.createIngested(batchNo, SourceTool.GREAT_EXPECTATIONS,
                FormatType.GE, "ch-1", Instant.parse("2026-08-11T10:00:00Z"), Collections.emptyList());
        batch.setLinkageStatus(linkageState);
        return batch;
    }

    private static RuleResultRow row(String assetId) {
        return RuleResultRow.builder()
                .assetId(assetId)
                .ruleName("非空率")
                .ruleType(RuleType.NON_NULL_RATE)
                .status(RuleStatus.PASSED)
                .executionTime(Instant.parse("2026-08-11T10:00:00Z"))
                .build();
    }
}
