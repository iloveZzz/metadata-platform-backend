package com.yss.metadata.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.metadata.domain.collector.gateway.AssetGateway;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.domain.collector.model.SavedAssetRef;
import com.yss.metadata.domain.collector.model.SavedColumnRef;
import com.yss.metadata.repository.AssetColumnRepository;
import com.yss.metadata.repository.AssetRepository;
import com.yss.metadata.repository.AssetVersionRepository;
import com.yss.metadata.infrastructure.convertor.AssetConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * 资产入库网关实现（MyBatis-Plus，幂等 upsert + 列全量替换 + 版本快照）。
 *
 * <p>幂等语义：同 source_id + name 的资产更新（列全量替换，字段强制覆写含 null），
 * 不存在则插入（初始状态 pending）；每轮保存生成版本快照（version 递增，schema_diff
 * 记录列快照文本）。</p>
 *
 * <p>事务边界（受控偏离，已登记）：{@code saveAssets} 使用 REQUIRES_NEW 独立事务，
 * 保证资产写入整体原子（任一资产失败则全部回滚，不残留部分资产 + 版本行）；
 * 由采集编排捕获异常后标记任务失败并提交任务状态。此偏离对合同"采集执行单聚合事务
 * （任务状态 + 资产/版本写入）"的诠释：任务状态与资产写入仍同一逻辑编排单元，
 * 资产写入以独立事务保证原子性（避免"任务失败 + 部分资产已入库"不一致）。</p>
 */
@Repository
public class AssetGatewayImpl implements AssetGateway {

    private final AssetRepository assetRepository;
    private final AssetColumnRepository assetColumnRepository;
    private final AssetVersionRepository assetVersionRepository;
    private final AssetConvertor assetConvertor;

    @Autowired
    public AssetGatewayImpl(AssetRepository assetRepository,
                            AssetColumnRepository assetColumnRepository,
                            AssetVersionRepository assetVersionRepository) {
        this(assetRepository, assetColumnRepository, assetVersionRepository, Mappers.getMapper(AssetConvertor.class));
    }

    public AssetGatewayImpl(AssetRepository assetRepository,
                            AssetColumnRepository assetColumnRepository,
                            AssetVersionRepository assetVersionRepository,
                            AssetConvertor assetConvertor) {
        this.assetRepository = assetRepository;
        this.assetColumnRepository = assetColumnRepository;
        this.assetVersionRepository = assetVersionRepository;
        this.assetConvertor = assetConvertor != null ? assetConvertor : Mappers.getMapper(AssetConvertor.class);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public List<SavedAssetRef> saveAssets(String sourceId, List<CollectedAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return new ArrayList<>();
        }
        List<SavedAssetRef> saved = new ArrayList<>();
        for (CollectedAsset asset : assets) {
            if (asset.getName() == null || asset.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("资产名称不能为空");
            }
            String assetId = upsertAsset(sourceId, asset);
            List<SavedColumnRef> columnRefs = replaceColumns(assetId, asset.getColumns());
            insertVersion(assetId, asset);
            saved.add(SavedAssetRef.builder()
                    .assetId(assetId)
                    .name(asset.getName())
                    .columns(columnRefs)
                    .build());
        }
        return saved;
    }

    private String upsertAsset(String sourceId, CollectedAsset asset) {
        AssetPO existing = assetRepository.selectOne(Wrappers.<AssetPO>lambdaQuery()
                .eq(AssetPO::getSourceId, sourceId)
                .eq(AssetPO::getName, asset.getName()));
        LocalDateTime now = LocalDateTime.now();
        String version = asset.getVersion();
        if (version == null || version.trim().isEmpty()) {
            version = "V" + DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").format(now);
        }

        if (existing == null) {
            String assetId = UUID.randomUUID().toString();
            AssetPO po = assetConvertor.toPO(asset);
            po.setId(assetId);
            po.setSourceId(sourceId);
            po.setDatabaseName(asset.getDatabaseName());
            po.setSourceSystem(asset.getSourceSystem());
            po.setCollectorTaskId(asset.getCollectorTaskId());
            po.setIsExcluded(false);
            po.setStatus("pending");
            po.setVersion(version);
            po.setUpdatedAt(now);
            assetRepository.insert(po);
            return assetId;
        }
        // 全量覆写语义：字段强制 set（含 null），保留原有的 isExcluded 标记
        assetRepository.update(null, Wrappers.<AssetPO>lambdaUpdate()
                .eq(AssetPO::getId, existing.getId())
                .set(AssetPO::getType, asset.getType())
                .set(AssetPO::getDomain, asset.getDomain())
                .set(AssetPO::getOwner, asset.getOwner())
                .set(AssetPO::getClassification, asset.getClassification())
                .set(AssetPO::getDatabaseName, asset.getDatabaseName() != null ? asset.getDatabaseName() : existing.getDatabaseName())
                .set(AssetPO::getSourceSystem, asset.getSourceSystem() != null ? asset.getSourceSystem() : existing.getSourceSystem())
                .set(AssetPO::getCollectorTaskId, asset.getCollectorTaskId() != null ? asset.getCollectorTaskId() : existing.getCollectorTaskId())
                .set(AssetPO::getDescription, (asset.getDescription() != null && !asset.getDescription().trim().isEmpty())
                        ? asset.getDescription() : existing.getDescription())
                .set(AssetPO::getRowCount, asset.getRowCount() != null ? asset.getRowCount() : existing.getRowCount())
                .set(AssetPO::getStorageSize, asset.getStorageSize() != null ? asset.getStorageSize() : existing.getStorageSize())
                .set(AssetPO::getVersion, version)
                .set(AssetPO::getUpdatedAt, now));
        return existing.getId();
    }

    private List<SavedColumnRef> replaceColumns(String assetId, List<CollectedColumn> columns) {
        assetColumnRepository.delete(Wrappers.<AssetColumnPO>lambdaQuery()
                .eq(AssetColumnPO::getAssetId, assetId));
        List<SavedColumnRef> refs = new ArrayList<>();
        if (columns == null) {
            return refs;
        }
        for (int i = 0; i < columns.size(); i++) {
            CollectedColumn column = columns.get(i);
            AssetColumnPO po = assetConvertor.toColumnPO(column);
            po.setId(UUID.randomUUID().toString());
            po.setAssetId(assetId);
            if (po.getOrdinalPosition() == null) {
                po.setOrdinalPosition(column.getOrdinalPosition() != null ? column.getOrdinalPosition() : (i + 1));
            }
            assetColumnRepository.insert(po);
            refs.add(SavedColumnRef.builder()
                    .columnId(po.getId())
                    .name(column.getName())
                    .comment(column.getComment())
                    .build());
        }
        return refs;
    }

    private void insertVersion(String assetId, CollectedAsset asset) {
        AssetVersionPO versionPo = new AssetVersionPO();
        versionPo.setId(UUID.randomUUID().toString());
        versionPo.setAssetId(assetId);
        versionPo.setVersion(nextVersion(assetId));
        versionPo.setSchemaDiff(schemaSnapshot(asset));
        versionPo.setCreatedAt(LocalDateTime.now());
        assetVersionRepository.insert(versionPo);
    }

    private int nextVersion(String assetId) {
        AssetVersionPO latest = assetVersionRepository.selectOne(Wrappers.<AssetVersionPO>lambdaQuery()
                .eq(AssetVersionPO::getAssetId, assetId)
                .orderByDesc(AssetVersionPO::getVersion)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    private String schemaSnapshot(CollectedAsset asset) {
        if (asset.getColumns() == null || asset.getColumns().isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(";");
        for (CollectedColumn column : asset.getColumns()) {
            joiner.add(column.getName() + "|" + column.getType()
                    + "|" + (column.getComment() == null ? "" : column.getComment()));
        }
        return joiner.toString();
    }
}
