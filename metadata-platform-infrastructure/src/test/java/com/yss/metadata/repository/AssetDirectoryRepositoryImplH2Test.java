package com.yss.metadata.repository;

import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.asset.model.AssetVersion;
import com.yss.metadata.infrastructure.convertor.AssetDirectoryConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.AssetVersionPO;
import com.yss.metadata.repository.gateway.impl.AssetDirectoryRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资产仓储网关持久化集成测试（目录上下文，H2 内存库替代真实 MySQL）。
 *
 * <p>验证资产读写（upsert）、收藏幂等切换（add/remove）、标签覆盖式更新、
 * 详情组合读取（字段/版本/数据源名称）。</p>
 */
class AssetDirectoryRepositoryImplH2Test extends H2MapperTestSupport {

    /** 目录上下文端口（与 MyBatis Mapper AssetRepository 同名，此处用全限定名区分） */
    private com.yss.metadata.domain.asset.gateway.AssetRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AssetDirectoryRepositoryImpl(
                sqlSession.getMapper(AssetRepository.class),
                sqlSession.getMapper(AssetColumnRepository.class),
                sqlSession.getMapper(AssetVersionRepository.class),
                sqlSession.getMapper(AssetFavoriteRepository.class),
                sqlSession.getMapper(AssetTagRepository.class),
                sqlSession.getMapper(ConnectorRepository.class),
                Mappers.getMapper(AssetDirectoryConvertor.class));
    }

    @Test
    @DisplayName("findById：返回资产并解析数据源名称")
    void findByIdResolvesSourceName() {
        sqlSession.getMapper(ConnectorRepository.class)
                .insert(buildConnectorPo("s-1", "交易中心主库"));
        sqlSession.getMapper(AssetRepository.class)
                .insert(buildAssetPo("a-1", "s-1", "dwd_trade_order_di", "table", "交易域",
                        "u-1", "内部", "claimed", LocalDateTime.of(2026, 8, 10, 9, 12)));

        Optional<Asset> asset = repository.findById("a-1");

        assertThat(asset).isPresent();
        assertThat(asset.get().getSourceName()).isEqualTo("交易中心主库");
        assertThat(asset.get().getStatus()).isEqualTo(AssetStatus.CLAIMED);
        assertThat(asset.get().getOwner()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("findById：不存在返回 empty")
    void findByIdMissingReturnsEmpty() {
        assertThat(repository.findById("not-exist")).isEmpty();
    }

    @Test
    @DisplayName("save：不存在则插入（含状态持久化）")
    void saveInsertsNewAsset() {
        Asset asset = asset("a-1", AssetStatus.PENDING, null);

        repository.save(asset);

        AssetPO po = sqlSession.getMapper(AssetRepository.class).selectById("a-1");
        assertThat(po).isNotNull();
        assertThat(po.getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("save：已存在则更新（认领/归档状态机持久化）")
    void saveUpdatesExistingAsset() {
        sqlSession.getMapper(AssetRepository.class)
                .insert(buildAssetPo("a-1", "s-1", "dwd_trade_order_di", "table", "交易域",
                        null, "内部", "pending", LocalDateTime.of(2026, 8, 10, 9, 12)));
        Asset asset = repository.findById("a-1").orElseThrow(AssertionError::new);
        asset.claim("u-1");

        repository.save(asset);

        AssetPO po = sqlSession.getMapper(AssetRepository.class).selectById("a-1");
        assertThat(po.getStatus()).isEqualTo("claimed");
        assertThat(po.getOwner()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("收藏：add 幂等（重复添加无异常、不重复计数），remove 幂等")
    void favoriteAddRemoveIdempotent() {
        repository.addFavorite("a-1", "u-me");
        repository.addFavorite("a-1", "u-me");
        assertThat(repository.isFavorite("a-1", "u-me")).isTrue();
        assertThat(sqlSession.getMapper(AssetFavoriteRepository.class).selectCount(null)).isEqualTo(1);

        repository.removeFavorite("a-1", "u-me");
        repository.removeFavorite("a-1", "u-me");
        assertThat(repository.isFavorite("a-1", "u-me")).isFalse();
    }

    @Test
    @DisplayName("收藏：按用户隔离（不同用户互不影响）")
    void favoriteIsPerUser() {
        repository.addFavorite("a-1", "u-me");
        repository.addFavorite("a-2", "u-other");

        assertThat(repository.isFavorite("a-1", "u-me")).isTrue();
        assertThat(repository.isFavorite("a-1", "u-other")).isFalse();
    }

    @Test
    @DisplayName("标签：覆盖式更新（先删后插，旧标签不残留）")
    void tagsReplaceAll() {
        repository.replaceTags("a-1", Arrays.asList("核心表", "日增量"));
        repository.replaceTags("a-1", Arrays.asList("核心表", "重要"));

        assertThat(repository.findTags("a-1")).containsExactlyInAnyOrder("核心表", "重要");
        assertThat(repository.findTags("a-1")).doesNotContain("日增量");
    }

    @Test
    @DisplayName("标签：空列表清空全部标签")
    void tagsEmptyClearsAll() {
        repository.replaceTags("a-1", Arrays.asList("核心表", "日增量"));

        repository.replaceTags("a-1", null);
        assertThat(repository.findTags("a-1")).isEmpty();
    }

    @Test
    @DisplayName("详情组合：字段清单与版本记录按序返回（按物理序号 ordinalPosition 排序）")
    void findColumnsAndVersions() {
        seedColumn("c-2", "a-1", "amount", "decimal(18,2)", "金额", Boolean.FALSE, "内部", 2);
        seedColumn("c-1", "a-1", "order_id", "bigint", "订单ID", Boolean.TRUE, "内部", 1);
        seedVersion("v-1", "a-1", 1, "初始");
        seedVersion("v-2", "a-1", 2, "新增列 customer_phone");

        List<AssetColumn> columns = repository.findColumns("a-1");
        assertThat(columns).extracting(AssetColumn::getName).containsExactly("order_id", "amount");
        assertThat(columns).extracting(AssetColumn::getOrdinalPosition).containsExactly(1, 2);

        List<AssetVersion> versions = repository.findVersions("a-1");
        assertThat(versions).extracting(AssetVersion::getVersion).containsExactly(2, 1);
    }

    private Asset asset(String id, AssetStatus status, String owner) {
        return Asset.builder()
                .id(id)
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

    private void seedColumn(String id, String assetId, String name, String type, String comment,
                            Boolean pk, String classification, Integer ordinalPosition) {
        AssetColumnPO po = new AssetColumnPO();
        po.setId(id);
        po.setAssetId(assetId);
        po.setName(name);
        po.setType(type);
        po.setComment(comment);
        po.setPk(pk);
        po.setClassification(classification);
        po.setOrdinalPosition(ordinalPosition);
        sqlSession.getMapper(AssetColumnRepository.class).insert(po);
    }

    private void seedVersion(String id, String assetId, int version, String schemaDiff) {
        AssetVersionPO po = new AssetVersionPO();
        po.setId(id);
        po.setAssetId(assetId);
        po.setVersion(version);
        po.setSchemaDiff(schemaDiff);
        po.setCreatedAt(LocalDateTime.of(2026, 8, 10, 9, 12));
        sqlSession.getMapper(AssetVersionRepository.class).insert(po);
    }
}
