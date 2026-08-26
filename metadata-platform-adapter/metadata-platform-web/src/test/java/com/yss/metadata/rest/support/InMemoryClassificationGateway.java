package com.yss.metadata.rest.support;

import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.model.Classification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分级分类结果仓储内存实现（Web 契约测试 seam，与 application 测试替身一致）。
 */
public class InMemoryClassificationGateway implements ClassificationGateway {

    private final Map<String, Classification> store = new LinkedHashMap<>();

    private final Map<String, String> columnOwner = new LinkedHashMap<>();

    public void seed(Classification classification) {
        store.put(classification.getId(), classification);
    }

    public void seedColumnOwner(String columnId, String assetId) {
        columnOwner.put(columnId, assetId);
    }

    @Override
    public List<Classification> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Classification> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Classification save(Classification classification) {
        store.put(classification.getId(), classification);
        return classification;
    }

    @Override
    public boolean saveCandidate(Classification candidate) {
        for (Classification existing : store.values()) {
            if (eq(existing.getAssetId(), candidate.getAssetId())
                    && eq(existing.getColumnId(), candidate.getColumnId())
                    && eq(existing.getName(), candidate.getName())) {
                return false;
            }
        }
        store.put(candidate.getId(), candidate);
        return true;
    }

    @Override
    public Optional<String> resolveSourceAssetId(Classification classification) {
        if (classification.getAssetId() != null && !classification.getAssetId().trim().isEmpty()) {
            return Optional.of(classification.getAssetId());
        }
        if (classification.getColumnId() == null || classification.getColumnId().trim().isEmpty()) {
            return Optional.empty();
        }
        String assetId = columnOwner.get(classification.getColumnId());
        return assetId == null ? Optional.empty() : Optional.of(assetId);
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
