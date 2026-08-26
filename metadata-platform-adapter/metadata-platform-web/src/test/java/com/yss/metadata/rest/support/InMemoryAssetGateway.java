package com.yss.metadata.rest.support;

import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.collector.model.SavedColumnRef;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 资产入库网关测试替身（Web 契约测试用，与 application 测试替身一致；
 * 切片 04 起 saveAssets 返回已入库资产/列引用）。
 */
public class InMemoryAssetGateway implements AssetGateway {

    private final List<SavedBatch> saved = new ArrayList<>();

    private RuntimeException failure;

    public void setFailure(RuntimeException failure) {
        this.failure = failure;
    }

    public List<SavedBatch> getSaved() {
        return saved;
    }

    @Override
    public List<SavedAssetRef> saveAssets(String sourceId, List<CollectedAsset> assets) {
        if (failure != null) {
            throw failure;
        }
        saved.add(new SavedBatch(sourceId, assets));
        List<SavedAssetRef> refs = new ArrayList<>();
        for (CollectedAsset asset : assets) {
            List<SavedColumnRef> columns = new ArrayList<>();
            if (asset.getColumns() != null) {
                int i = 0;
                for (CollectedColumn column : asset.getColumns()) {
                    columns.add(SavedColumnRef.builder()
                            .columnId("col-" + asset.getName() + "-" + i)
                            .name(column.getName())
                            .comment(column.getComment())
                            .build());
                    i++;
                }
            }
            refs.add(SavedAssetRef.builder()
                    .assetId("asset-" + asset.getName())
                    .name(asset.getName())
                    .columns(columns)
                    .build());
        }
        return refs;
    }

    @Getter
    @AllArgsConstructor
    public static class SavedBatch {
        private final String sourceId;
        private final List<CollectedAsset> assets;
    }
}
