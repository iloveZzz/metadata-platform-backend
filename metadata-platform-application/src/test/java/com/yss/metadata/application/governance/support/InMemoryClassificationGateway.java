package com.yss.metadata.application.governance.support;

import com.yss.metadata.domain.governance.gateway.ClassificationGateway;
import com.yss.metadata.domain.governance.model.Classification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分级分类结果仓储内存实现（应用/契约测试 seam）。
 *
 * <p>镜像生产语义：saveCandidate 同 assetId+columnId+name 幂等跳过；
 * resolveSourceAssetId 优先取 assetId，缺省经列反查父资产。</p>
 */
public class InMemoryClassificationGateway implements ClassificationGateway {

    private final Map<String, Classification> store = new LinkedHashMap<>();

    private final Map<String, String> columnOwner = new LinkedHashMap<>();

    public void seed(Classification classification) {
        store.put(classification.getId(), classification);
    }

    /** 登记列 → 父资产（resolveSourceAssetId 反查用）。 */
    public void seedColumnOwner(String columnId, String assetId) {
        columnOwner.put(columnId, assetId);
    }

    public Map<String, Classification> store() {
        return store;
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
                return false; // 幂等：同 asset+column+name 已存在跳过
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
