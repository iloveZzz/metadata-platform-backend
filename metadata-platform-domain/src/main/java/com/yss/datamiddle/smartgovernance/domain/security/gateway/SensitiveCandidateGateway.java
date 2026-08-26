package com.yss.datamiddle.smartgovernance.domain.security.gateway;

import com.yss.datamiddle.smartgovernance.domain.security.model.CandidateStatus;
import com.yss.datamiddle.smartgovernance.domain.security.model.SecurityLevel;
import com.yss.datamiddle.smartgovernance.domain.security.model.SensitiveCandidate;

import java.util.List;
import java.util.Optional;

public interface SensitiveCandidateGateway {
    void batchSave(List<SensitiveCandidate> candidates);

    Optional<SensitiveCandidate> findById(String id);

    List<SensitiveCandidate> findByIds(List<String> ids);

    void batchUpdate(List<SensitiveCandidate> candidates);

    List<SensitiveCandidate> queryCandidates(
            Integer pageIndex,
            Integer pageSize,
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    );

    long countCandidates(
            CandidateStatus status,
            SecurityLevel securityLevel,
            String sensitiveType,
            String keyword
    );
}
