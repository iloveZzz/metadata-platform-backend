package com.yss.metadata.domain.asset.model;

import com.yss.metadata.domain.asset.exception.AssetClaimConflictException;
import com.yss.metadata.domain.asset.exception.AssetStateConflictException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 元数据资产聚合根（数据架构 Asset；目录上下文）。
 *
 * <p>状态机：待认领 → 已认领 → 已归档（只读），已删除为采集端标记终态。
 * 核心规则（状态矩阵 / 交互说明 §7）：
 * <ul>
 *   <li>认领：owner 唯一——已被他人认领抛 {@link AssetClaimConflictException}（409）；本人重复认领幂等；</li>
 *   <li>归档：待认领/已认领 → 已归档；重复归档抛 {@link AssetStateConflictException}（409）；已删除不可归档；</li>
 *   <li>取消归档：已归档 → 恢复可编辑（有 owner 回已认领，无 owner 回待认领）；非归档状态幂等；</li>
 *   <li>归档后只读：编辑类操作（认领/标签/归档）经 {@link #ensureWritable(String)} 阻断。</li>
 * </ul></p>
 *
 * <p>查询组合字段（非持久化）：{@code sourceName}（数据源名称）、{@code favorite}（当前用户收藏）、
 * {@code tags}（标签）由查询/详情用例填充，供 VO 组装使用。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（OpenAPI 类型为 string，UUID） */
    private String id;

    /** 来源数据源 id（asset.source_id） */
    private String sourceId;

    /** 数据源名称（查询组合字段，非持久化） */
    private String sourceName;

    /** 资产名称（同数据源内唯一） */
    private String name;

    /** 资产类型：table / column / view */
    private String type;

    /** 数据域 */
    private String domain;

    /** 负责人（认领后唯一） */
    private String owner;

    /** 分级分类 */
    private String classification;

    /** 所属数据库名称 */
    private String databaseName;

    /** 所属 Schema 空间名称 */
    private String schemaName;

    /** 来源业务系统编码或名称 */
    private String sourceSystem;

    /** 最后一次采集的任务 ID */
    private String collectorTaskId;

    /** 采集任务名称（查询组合字段） */
    private String collectorName;

    /** 更新频率类型（查询组合字段，如 定时 / 手动） */
    private String updateFrequency;

    /** 调度人类可读描述（查询组合字段，如 每日, 04:11） */
    private String scheduleDescription;

    /** 是否已剔除/软删除 */
    @Builder.Default
    private Boolean isExcluded = false;

    /** 当前状态 */
    private AssetStatus status;

    /** 最新版本号（如 V2026.08.23.221530） */
    private String version;

    /** 最后更新时间 */
    private LocalDateTime updatedAt;

    /** 当前用户是否收藏（查询组合字段，非持久化） */
    private Boolean favorite;

    /** 标签列表（查询组合字段，非持久化） */
    private List<String> tags;

    /** 数据存疑状态：NORMAL / TAINTED（切片 08 新增） */
    @Builder.Default
    private String taintStatus = "NORMAL";

    /** 质量健康分（0~100，查询组合字段） */
    private Integer healthScore;

    /** 质量梯度：excellent / good / fair / poor */
    private String qualityBand;

    /** 数据源类型（如 MySQL / Oracle） */
    private String datasourceType;

    /** 元数据描述/表注释 */
    private String description;

    /** 表物理行数 */
    private Long rowCount;

    /** 表存储大小（如 12.03MB） */
    private String storageSize;

    /**
     * 剔除/软删除：标记为已剔除。
     */
    public void exclude() {
        ensureWritable("剔除");
        this.isExcluded = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 恢复：将已剔除资产恢复为正常清单。
     */
    public void recover() {
        this.isExcluded = false;
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * 认领：owner 唯一。
     *
     * <p>已被他人认领抛认领冲突（409 语义）；本人重复认领幂等无操作；
     * 待认领（owner 空）认领后置 owner 并流转为已认领。</p>
     */
    public void claim(String currentUser) {
        if (status == AssetStatus.DELETED) {
            throw new AssetStateConflictException("已删除资产不可认领");
        }
        if (status == AssetStatus.ARCHIVED) {
            throw new AssetStateConflictException("已归档资产只读，不可认领");
        }
        if (owner != null && !owner.trim().isEmpty() && !owner.equals(currentUser)) {
            throw new AssetClaimConflictException("资产已被 " + owner + " 认领，当前用户无权认领");
        }
        if (owner == null || owner.trim().isEmpty()) {
            this.owner = currentUser;
            this.status = AssetStatus.CLAIMED;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 归档：待认领/已认领 → 已归档（只读）。
     *
     * <p>重复归档抛状态冲突（409 语义，对齐交互说明 §7「已归档拒绝重复」）；
     * 已删除资产不可归档。</p>
     */
    public void archive() {
        if (status == AssetStatus.DELETED) {
            throw new AssetStateConflictException("已删除资产不可归档");
        }
        if (status == AssetStatus.ARCHIVED) {
            throw new AssetStateConflictException("资产已归档，禁止重复归档");
        }
        this.status = AssetStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消归档：已归档 → 恢复可编辑。
     *
     * <p>有 owner 回已认领、无 owner 回待认领（保持归档前 owner 语义）；
     * 非归档状态幂等无操作（交互说明 §7：取消归档幂等）。</p>
     */
    public void unarchive() {
        if (status == AssetStatus.DELETED) {
            throw new AssetStateConflictException("已删除资产不可取消归档");
        }
        if (status == AssetStatus.ARCHIVED) {
            this.status = (owner != null && !owner.trim().isEmpty())
                    ? AssetStatus.CLAIMED : AssetStatus.PENDING;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 编辑类操作只读拦截（归档后只读）。
     *
     * <p>已归档抛状态冲突（如编辑标签）；已删除抛状态冲突；
     * 待认领/已认领放行。</p>
     *
     * @param action 操作名（用于错误文案，如"编辑标签"）
     */
    public void ensureWritable(String action) {
        if (status == AssetStatus.DELETED) {
            throw new AssetStateConflictException("已删除资产不可" + action);
        }
        if (status == AssetStatus.ARCHIVED) {
            throw new AssetStateConflictException("已归档资产只读，" + action + "被禁用");
        }
    }
}
