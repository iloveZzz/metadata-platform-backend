package com.yss.metadata.application.asset.service;

import com.yss.metadata.application.asset.service.convertor.AssetAppConvertor;
import com.yss.metadata.application.asset.service.impl.AssetActionServiceImpl;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.client.vo.AssetVO;
import com.yss.metadata.domain.asset.exception.AssetClaimConflictException;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.exception.AssetStateConflictException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资产操作用例应用服务测试（WU-02-02）。
 *
 * <p>覆盖：收藏幂等切换（含归档资产可收藏/已删除阻断）、认领 owner 唯一
 * （冲突 409 语义/本人幂等）、标签覆盖式更新与归一化（归档阻断）、
 * 归档-取消归档只读状态机、404 语义。</p>
 */
class AssetActionServiceTest {

    private InMemoryAssetRepository repository;
    private AssetActionService actionService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAssetRepository();
        actionService = new AssetActionServiceImpl(repository, org.mapstruct.factory.Mappers.getMapper(AssetAppConvertor.class));
    }

    // ---------- 收藏 ----------

    @Test
    @DisplayName("未收藏资产收藏成功：返回 VO favorite=true 且已持久化")
    void toggleFavoriteAdds() {
        Asset asset = seedAsset("a-1", AssetStatus.CLAIMED, "u-1");

        AssetVO vo = actionService.toggleFavorite("a-1", "u-me");

        assertThat(vo.getFavorite()).isTrue();
        assertThat(repository.isFavorite("a-1", "u-me")).isTrue();
    }

    @Test
    @DisplayName("已收藏资产再次收藏切换为取消：返回 VO favorite=false")
    void toggleFavoriteRemoves() {
        Asset asset = seedAsset("a-1", AssetStatus.CLAIMED, "u-1");
        repository.seedFavorite("a-1", "u-me");

        AssetVO vo = actionService.toggleFavorite("a-1", "u-me");

        assertThat(vo.getFavorite()).isFalse();
        assertThat(repository.isFavorite("a-1", "u-me")).isFalse();
    }

    @Test
    @DisplayName("归档资产仍可收藏（收藏非编辑操作，状态矩阵禁用列表不含收藏）")
    void toggleFavoriteOnArchivedAllowed() {
        seedAsset("a-1", AssetStatus.ARCHIVED, "u-1");

        AssetVO vo = actionService.toggleFavorite("a-1", "u-me");

        assertThat(vo.getFavorite()).isTrue();
        assertThat(repository.isFavorite("a-1", "u-me")).isTrue();
    }

    @Test
    @DisplayName("已删除资产收藏被阻断（409 语义）")
    void toggleFavoriteOnDeletedThrows() {
        seedAsset("a-1", AssetStatus.DELETED, null);

        assertThatThrownBy(() -> actionService.toggleFavorite("a-1", "u-me"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    @Test
    @DisplayName("收藏不存在的资产抛未找到（404 语义）")
    void toggleFavoriteNotFoundThrows() {
        assertThatThrownBy(() -> actionService.toggleFavorite("not-exist", "u-me"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    // ---------- 认领 ----------

    @Test
    @DisplayName("待认领资产认领成功：owner 持久化，状态流转为已认领")
    void claimSuccess() {
        Asset asset = seedAsset("a-1", AssetStatus.PENDING, null);

        AssetVO vo = actionService.claim("a-1", "u-me");

        assertThat(vo.getOwner()).isEqualTo("u-me");
        assertThat(vo.getStatus()).isEqualTo("claimed");
        assertThat(repository.findById("a-1").orElseThrow(AssertionError::new).getOwner())
                .isEqualTo("u-me");
    }

    @Test
    @DisplayName("已被他人认领的资产认领抛认领冲突（409 语义）")
    void claimConflictThrows() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-other");

        assertThatThrownBy(() -> actionService.claim("a-1", "u-me"))
                .isInstanceOf(AssetClaimConflictException.class)
                .hasMessageContaining("已被");
    }

    @Test
    @DisplayName("本人重复认领幂等：不报错且 owner 不变")
    void claimBySameOwnerIdempotent() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-me");

        AssetVO vo = actionService.claim("a-1", "u-me");

        assertThat(vo.getOwner()).isEqualTo("u-me");
        assertThat(vo.getStatus()).isEqualTo("claimed");
    }

    @Test
    @DisplayName("已归档资产认领被阻断（归档只读，409 语义）")
    void claimArchivedThrows() {
        seedAsset("a-1", AssetStatus.ARCHIVED, "u-other");

        assertThatThrownBy(() -> actionService.claim("a-1", "u-me"))
                .isInstanceOf(AssetStateConflictException.class);
    }

    @Test
    @DisplayName("认领不存在的资产抛未找到（404 语义）")
    void claimNotFoundThrows() {
        assertThatThrownBy(() -> actionService.claim("not-exist", "u-me"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    // ---------- 标签 ----------

    @Test
    @DisplayName("标签覆盖式更新：旧标签被全量替换（不含旧值残留）")
    void updateTagsReplacesAll() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-1");
        repository.seedTags("a-1", Arrays.asList("核心表", "日增量"));

        actionService.updateTags("a-1", Arrays.asList("核心表", "重要"));

        List<String> tags = repository.findTags("a-1");
        assertThat(tags).containsExactlyInAnyOrder("核心表", "重要");
        assertThat(tags).doesNotContain("日增量");
    }

    @Test
    @DisplayName("空标签列表表示清空全部标签")
    void updateTagsEmptyClearsAll() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-1");
        repository.seedTags("a-1", Arrays.asList("核心表", "日增量"));

        actionService.updateTags("a-1", Collections.emptyList());

        assertThat(repository.findTags("a-1")).isEmpty();
    }

    @Test
    @DisplayName("标签归一化：trim 与去重（保序），空白元素丢弃")
    void updateTagsNormalizes() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-1");

        actionService.updateTags("a-1", Arrays.asList(" 核心表 ", "核心表", "  ", null, "重要"));

        assertThat(repository.findTags("a-1")).containsExactly("核心表", "重要");
    }

    @Test
    @DisplayName("已归档资产编辑标签被阻断（归档只读，409 语义）")
    void updateTagsOnArchivedThrows() {
        seedAsset("a-1", AssetStatus.ARCHIVED, "u-1");

        assertThatThrownBy(() -> actionService.updateTags("a-1", Collections.singletonList("核心表")))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("只读");
    }

    @Test
    @DisplayName("已删除资产编辑标签被阻断（409 语义）")
    void updateTagsOnDeletedThrows() {
        seedAsset("a-1", AssetStatus.DELETED, null);

        assertThatThrownBy(() -> actionService.updateTags("a-1", Collections.singletonList("核心表")))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("已删除");
    }

    // ---------- 归档 / 取消归档 ----------

    @Test
    @DisplayName("归档成功：状态流转为已归档并持久化")
    void archiveSuccess() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-1");

        AssetVO vo = actionService.archive("a-1");

        assertThat(vo.getStatus()).isEqualTo("archived");
        assertThat(repository.findById("a-1").orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(AssetStatus.ARCHIVED);
    }

    @Test
    @DisplayName("重复归档抛状态冲突（409 语义）")
    void reArchiveThrows() {
        seedAsset("a-1", AssetStatus.ARCHIVED, "u-1");

        assertThatThrownBy(() -> actionService.archive("a-1"))
                .isInstanceOf(AssetStateConflictException.class)
                .hasMessageContaining("重复归档");
    }

    @Test
    @DisplayName("取消归档成功：已归档恢复为已认领并持久化")
    void unarchiveSuccess() {
        seedAsset("a-1", AssetStatus.ARCHIVED, "u-1");

        AssetVO vo = actionService.unarchive("a-1");

        assertThat(vo.getStatus()).isEqualTo("claimed");
        assertThat(repository.findById("a-1").orElseThrow(AssertionError::new).getStatus())
                .isEqualTo(AssetStatus.CLAIMED);
    }

    @Test
    @DisplayName("未归档状态取消归档幂等：不报错状态不变")
    void unarchiveActiveIdempotent() {
        seedAsset("a-1", AssetStatus.CLAIMED, "u-1");

        AssetVO vo = actionService.unarchive("a-1");

        assertThat(vo.getStatus()).isEqualTo("claimed");
    }

    @Test
    @DisplayName("已删除资产归档/取消归档均抛状态冲突（409 语义）")
    void archiveDeletedThrows() {
        seedAsset("a-1", AssetStatus.DELETED, null);

        assertThatThrownBy(() -> actionService.archive("a-1"))
                .isInstanceOf(AssetStateConflictException.class);
        assertThatThrownBy(() -> actionService.unarchive("a-1"))
                .isInstanceOf(AssetStateConflictException.class);
    }

    private Asset seedAsset(String id, AssetStatus status, String owner) {
        Asset asset = Asset.builder()
                .id(id)
                .sourceId("s-1")
                .sourceName("交易中心主库")
                .name("dwd_trade_order_di")
                .type("table")
                .domain("交易域")
                .owner(owner)
                .classification("内部")
                .status(status)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12))
                .build();
        repository.seed(asset);
        return asset;
    }
}
