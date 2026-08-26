package com.yss.smartdiscovery.domain.gateway;

import com.yss.smartdiscovery.domain.candidate.SmartTagCandidate;

import java.util.List;
import java.util.Optional;

public interface CandidateRepository {
    void saveAll(List<SmartTagCandidate> candidates);
    List<SmartTagCandidate> listCandidates(String status, String source, String domain);
    Optional<SmartTagCandidate> findById(String id);
    void update(SmartTagCandidate candidate);
    void updateBatchStatus(List<String> ids, String status);
}
