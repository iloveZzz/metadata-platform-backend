package com.yss.metadata.domain.asset;

import com.yss.metadata.domain.asset.exception.AssetClaimConflictException;
import com.yss.metadata.domain.asset.exception.AssetStateConflictException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资产聚合行为测试（WU-02-02 状态机部分）。
 *
 * <p>覆盖：认领 owner 唯一（冲突 409 语义/本人幂等）、归档-取消归档状态机
 * （重复归档 409、归档只读、已删除阻断）、编辑类操作只读拦截。</p>
 */
class AssetTest {

    // ---------- 认领 ----------

    @Test
    @DisplayName("待认领资产认领成功：owner 置为当前用户，状态流转为已认领")
    void claimPendingMarksClaimed() {
        Asset asset = buildAsset(AssetStatus.PENDING, null);

        asset.claim("u-1");

        assertThat(asset.getOwner()).isEqualTo("u-1");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.CLAIMED);
        assertThat(asset.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("已被他人认领的资产认领抛认领冲突（409 语义）")
    void claimByOtherOwnerThrowsConflict() {
        Asset asset = buildAsset(AssetStatus.CLAIMED, "u-other");

        assertThatThrownBy(() -> asset.claim("u-me"))
                .isInstanceOf(AssetClaimConflictException.class)
                .hasMessageContaining("已被");
    }

    @Test
    @DisplayName("本人重复认领幂等：不报错、owner 与状态保持不变")
    void claimBySameOwnerIsIdempotent() {
        Asset asset = buildAsset(AssetStatus.CLAIMED, "u-1");
        LocalDateTime before = asset.getUpdatedAt();

        asset.claim("u-1");

        assertThat(asset.getOwner()).isEqualTo("u-1");
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.CLAIMED);
        assertThat(asset.getUpdatedAt()).isEqualTo(before);
    }

    @Test
    @DisplayName("已归档资产认领被阻断（409 状态冲突，归档只读）")
    void claimArchivedThrowsStateConflict() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, "u-1");

        assertThatThrownBy(() -> asset.claim("u-2"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("只读");
    }

    @Test
    @DisplayName("已删除资产认领被阻断（409 状态冲突）")
    void claimDeletedThrowsStateConflict() {
        Asset asset = buildAsset(AssetStatus.DELETED, null);

        assertThatThrownBy(() -> asset.claim("u-1"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    // ---------- 归档 ----------

    @Test
    @DisplayName("已认领资产归档成功：状态流转为已归档")
    void archiveClaimedMarksArchived() {
        Asset asset = buildAsset(AssetStatus.CLAIMED, "u-1");

        asset.archive();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.ARCHIVED);
    }

    @Test
    @DisplayName("待认领资产可直接归档")
    void archivePendingMarksArchived() {
        Asset asset = buildAsset(AssetStatus.PENDING, null);

        asset.archive();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.ARCHIVED);
    }

    @Test
    @DisplayName("重复归档抛状态冲突（409 语义，对齐交互说明「已归档拒绝重复」）")
    void archiveArchivedThrowsStateConflict() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, "u-1");

        assertThatThrownBy(asset::archive)
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("重复归档");
    }

    @Test
    @DisplayName("已删除资产归档抛状态冲突（409 语义）")
    void archiveDeletedThrowsStateConflict() {
        Asset asset = buildAsset(AssetStatus.DELETED, null);

        assertThatThrownBy(asset::archive)
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    // ---------- 取消归档 ----------

    @Test
    @DisplayName("取消归档：有 owner 恢复为已认领（可编辑）")
    void unarchiveWithOwnerRestoresClaimed() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, "u-1");

        asset.unarchive();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.CLAIMED);
        assertThat(asset.getOwner()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("取消归档：无 owner（归档前待认领）恢复为待认领")
    void unarchiveWithoutOwnerRestoresPending() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, null);

        asset.unarchive();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.PENDING);
    }

    @Test
    @DisplayName("未归档状态取消归档幂等：不报错、状态不变")
    void unarchiveNonArchivedIsIdempotent() {
        Asset asset = buildAsset(AssetStatus.CLAIMED, "u-1");
        LocalDateTime before = asset.getUpdatedAt();

        asset.unarchive();

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.CLAIMED);
        assertThat(asset.getUpdatedAt()).isEqualTo(before);
    }

    @Test
    @DisplayName("已删除资产取消归档抛状态冲突（409 语义）")
    void unarchiveDeletedThrowsStateConflict() {
        Asset asset = buildAsset(AssetStatus.DELETED, null);

        assertThatThrownBy(asset::unarchive)
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    // ---------- 编辑类操作只读拦截 ----------

    @Test
    @DisplayName("已归档资产编辑类操作被阻断（归档只读）")
    void ensureWritableArchivedThrows() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, "u-1");

        assertThatThrownBy(() -> asset.ensureWritable("编辑标签"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("只读")
                .hasMessageContaining("编辑标签");
    }

    @Test
    @DisplayName("已删除资产编辑类操作被阻断")
    void ensureWritableDeletedThrows() {
        Asset asset = buildAsset(AssetStatus.DELETED, null);

        assertThatThrownBy(() -> asset.ensureWritable("编辑标签"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    @Test
    @DisplayName("待认领/已认领资产编辑类操作放行")
    void ensureWritableActivePasses() {
        buildAsset(AssetStatus.PENDING, null).ensureWritable("编辑标签");
        buildAsset(AssetStatus.CLAIMED, "u-1").ensureWritable("编辑标签");
    }

    // ---------- 剔除与恢复 ----------

    @Test
    @DisplayName("正常资产剔除成功：isExcluded 置为 true，更新时间刷新")
    void excludeMarksExcluded() {
        Asset asset = buildAsset(AssetStatus.PENDING, null);
        assertThat(asset.getIsExcluded()).isFalse();

        asset.exclude();

        assertThat(asset.getIsExcluded()).isTrue();
        assertThat(asset.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("已归档资产执行剔除抛状态冲突")
    void excludeArchivedThrowsConflict() {
        Asset asset = buildAsset(AssetStatus.ARCHIVED, null);

        assertThatThrownBy(asset::exclude)
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("只读");
    }

    @Test
    @DisplayName("已剔除资产恢复成功：isExcluded 置为 false")
    void recoverRestoresActive() {
        Asset asset = buildAsset(AssetStatus.PENDING, null);
        asset.exclude();
        assertThat(asset.getIsExcluded()).isTrue();

        asset.recover();

        assertThat(asset.getIsExcluded()).isFalse();
    }

    // ---------- 状态枚举 ----------

    @Test
    @DisplayName("状态列值 ↔ 枚举互转；未知值抛非法参数")
    void statusEnumRoundTrip() {
        assertThat(AssetStatus.fromValue("pending")).isEqualTo(AssetStatus.PENDING);
        assertThat(AssetStatus.fromValue("claimed")).isEqualTo(AssetStatus.CLAIMED);
        assertThat(AssetStatus.fromValue("archived")).isEqualTo(AssetStatus.ARCHIVED);
        assertThat(AssetStatus.fromValue("deleted")).isEqualTo(AssetStatus.DELETED);
        assertThat(AssetStatus.PENDING.getValue()).isEqualTo("pending");
        assertThatThrownBy(() -> AssetStatus.fromValue("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Asset buildAsset(AssetStatus status, String owner) {
        return Asset.builder()
                .id("a-1")
                .sourceId("s-1")
                .name("dwd_trade_order_di")
                .type("table")
                .domain("交易域")
                .owner(owner)
                .classification("内部")
                .status(status)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12))
                .build();
    }
}
