package com.yss.datamiddle.dqinsight.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * 资产关联（质量结果与主平台资产的映射，AssetLinkage）。
 *
 * <p>未命中（asset 404）→ state=pending 挂待关联队列（不阻断入库，SB-05）；人工映射属切片 04。</p>
 */
@Getter
@Setter
public class AssetLinkage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（保存后由持久化分配） */
    private Long id;

    /** 批次 ID */
    private Long batchId;

    /** 源资产 ID（结果中的资产 ID） */
    private String sourceAssetId;

    /** 解析后资产 ID（主平台口径；pending 时为 null） */
    private String resolvedAssetId;

    /** 资产名称快照 */
    private String assetName;

    /** 数据域快照 */
    private String domain;

    /** 资产类型快照 */
    private String assetType;

    /** 匹配方式（auto / manual） */
    private LinkageMatchMode matchMode;

    /** 关联状态 */
    private LinkageState state;

    /** 创建时间 */
    private Instant createdAt;

    /** 映射时间（人工映射后） */
    private Instant mappedAt;

    /** 映射人 */
    private String mappedBy;

    /** 备注 */
    private String note;

    private AssetLinkage() {
    }

    /**
     * 持久化 / 映射专用构造（仅 MapStruct 反向映射 toDomain 使用；字段经 setter 回填；
     * 业务创建必须使用 pending / linked 工厂）。
     */
    public static AssetLinkage forPersistenceLoad() {
        return new AssetLinkage();
    }

    /** 未命中：挂待关联队列 */
    public static AssetLinkage pending(DQResultBatch batch, String sourceAssetId) {
        AssetLinkage linkage = new AssetLinkage();
        linkage.sourceAssetId = sourceAssetId;
        linkage.state = LinkageState.PENDING;
        linkage.createdAt = Instant.now();
        linkage.note = "资产未命中，待人工映射";
        return linkage;
    }

    /** 命中：保存资产快照 */
    public static AssetLinkage linked(DQResultBatch batch, String sourceAssetId, AssetSnapshot snapshot) {
        AssetLinkage linkage = new AssetLinkage();
        linkage.sourceAssetId = sourceAssetId;
        linkage.resolvedAssetId = snapshot.getAssetId();
        linkage.assetName = snapshot.getAssetName();
        linkage.domain = snapshot.getDomain();
        linkage.assetType = snapshot.getAssetType();
        linkage.matchMode = LinkageMatchMode.AUTO;
        linkage.state = LinkageState.LINKED;
        linkage.createdAt = Instant.now();
        return linkage;
    }
}
