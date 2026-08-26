package com.yss.smartdiscovery.application.service;

import com.yss.smartdiscovery.application.dto.BatchSummaryDTO;
import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.gateway.CandidateRepository;
import com.yss.smartdiscovery.domain.gateway.TagRepository;
import com.yss.smartdiscovery.domain.rule.TaggingFunnelEngine;
import com.yss.smartdiscovery.domain.tag.SmartTagDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaggingFunnelAppService {

    private final TagRepository tagRepository;
    private final CandidateRepository candidateRepository;

    public BatchSummaryDTO runTaggingAnalysis(List<String> tableNames, String domainFilter) {
        List<SmartTagDefinition> availableTags = tagRepository.listTags(domainFilter);

        // 模拟提取表和字段元数据
        List<TaggingFunnelEngine.ColumnTarget> targets = new ArrayList<>();
        targets.add(TaggingFunnelEngine.ColumnTarget.builder().tableName("dwd_trade_order_di").columnName("cust_id_card").columnComment("客户身份证号").dataType("VARCHAR(18)").build());
        targets.add(TaggingFunnelEngine.ColumnTarget.builder().tableName("dwd_trade_order_di").columnName("mobile_phone").columnComment("用户手机号码").dataType("VARCHAR(20)").build());
        targets.add(TaggingFunnelEngine.ColumnTarget.builder().tableName("dwd_trade_order_di").columnName("trans_amount").columnComment("实际交易金额").dataType("DECIMAL(18,2)").build());
        targets.add(TaggingFunnelEngine.ColumnTarget.builder().tableName("dwd_cust_profile_df").columnName("annual_income_level").columnComment("年收入层级").dataType("VARCHAR(32)").build());
        targets.add(TaggingFunnelEngine.ColumnTarget.builder().tableName("ads_vip_trans_di").columnName("trans_amount_half_year").columnComment("近半年交易总额").dataType("DECIMAL(18,2)").build());

        List<SmartTagCandidate> results = TaggingFunnelEngine.analyze(targets, availableTags, 0.90);
        candidateRepository.saveAll(results);

        long autoCount = results.stream().filter(r -> "AUTO_APPLIED".equals(r.getStatus())).count();
        long pendingCount = results.stream().filter(r -> "PENDING".equals(r.getStatus())).count();

        return BatchSummaryDTO.builder()
                .batchId("BATCH-" + System.currentTimeMillis())
                .totalProcessed(results.size())
                .autoAppliedCount((int) autoCount)
                .pendingReviewCount((int) pendingCount)
                .build();
    }
}
