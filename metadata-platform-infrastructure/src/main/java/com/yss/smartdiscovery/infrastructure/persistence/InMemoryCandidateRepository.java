package com.yss.smartdiscovery.infrastructure.persistence;

import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;
import com.yss.smartdiscovery.domain.gateway.CandidateRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryCandidateRepository implements CandidateRepository {

    private final Map<String, SmartTagCandidate> candidateMap = new ConcurrentHashMap<>();

    public InMemoryCandidateRepository() {
        initDefaultCandidates();
    }

    private void initDefaultCandidates() {
        candidateMap.put("CAN-01", SmartTagCandidate.builder().id("CAN-01").tableName("dwd_trade_order_di").columnName("cust_id_card").columnComment("客户身份证号").recommendedTagId("TAG-01").recommendedTagName("L4 核心敏感数据").tagCategory("SECURITY").source("L1_RULE").confidence(0.98).status("AUTO_APPLIED").createdAt(LocalDateTime.now()).build());
        candidateMap.put("CAN-02", SmartTagCandidate.builder().id("CAN-02").tableName("dwd_trade_order_di").columnName("mobile_phone").columnComment("联系手机号").recommendedTagId("TAG-01").recommendedTagName("L4 核心敏感数据").tagCategory("SECURITY").source("L1_RULE").confidence(0.96).status("AUTO_APPLIED").createdAt(LocalDateTime.now()).build());
        candidateMap.put("CAN-03", SmartTagCandidate.builder().id("CAN-03").tableName("dwd_cust_profile_df").columnName("annual_income_level").columnComment("年收入层级").recommendedTagId("TAG-04").recommendedTagName("财富管理客户域").tagCategory("DOMAIN").source("L2_DICT").confidence(0.88).status("PENDING").createdAt(LocalDateTime.now()).build());
        candidateMap.put("CAN-04", SmartTagCandidate.builder().id("CAN-04").tableName("ads_vip_trans_di").columnName("trans_amount_half_year").columnComment("近半年交易总额").recommendedTagId("TAG-03").recommendedTagName("零售金融交易域").tagCategory("DOMAIN").source("L3_LLM").confidence(0.85).status("PENDING").createdAt(LocalDateTime.now()).build());
        candidateMap.put("CAN-05", SmartTagCandidate.builder().id("CAN-05").tableName("dwd_risk_blacklist_df").columnName("risk_score_val").columnComment("反欺诈风险分值").recommendedTagId("TAG-02").recommendedTagName("L3 敏感商业数据").tagCategory("SECURITY").source("L3_LLM").confidence(0.79).status("PENDING").createdAt(LocalDateTime.now()).build());
    }

    @Override
    public void saveAll(List<SmartTagCandidate> candidates) {
        for (SmartTagCandidate c : candidates) {
            candidateMap.put(c.getId(), c);
        }
    }

    @Override
    public List<SmartTagCandidate> listCandidates(String status, String source, String domain) {
        return candidateMap.values().stream()
                .filter(c -> status == null || "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(c.getStatus()))
                .filter(c -> source == null || "ALL".equalsIgnoreCase(source) || source.equalsIgnoreCase(c.getSource()))
                .filter(c -> domain == null || "ALL".equalsIgnoreCase(domain) || domain.equalsIgnoreCase(c.getTagCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SmartTagCandidate> findById(String id) {
        return Optional.ofNullable(candidateMap.get(id));
    }

    @Override
    public void update(SmartTagCandidate candidate) {
        candidateMap.put(candidate.getId(), candidate);
    }

    @Override
    public void updateBatchStatus(List<String> ids, String status) {
        for (String id : ids) {
            SmartTagCandidate c = candidateMap.get(id);
            if (c != null) {
                c.setStatus(status);
                c.setUpdatedAt(LocalDateTime.now());
            }
        }
    }
}
