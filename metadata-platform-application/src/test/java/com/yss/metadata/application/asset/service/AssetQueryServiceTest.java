package com.yss.metadata.application.asset.service;

import com.yss.metadata.application.asset.service.convertor.AssetAppConvertor;
import com.yss.metadata.application.asset.service.impl.AssetQueryServiceImpl;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.asset.support.InMemorySearchIndex;
import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.client.vo.AssetDetailVO;
import com.yss.metadata.client.vo.AssetPageVO;
import com.yss.metadata.client.vo.AssetVO;
import com.yss.metadata.domain.asset.exception.AssetNotFoundException;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.asset.model.AssetVersion;
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
 * 资产查询应用服务测试（WU-02-01 编排 / WU-02-03 详情聚合）。
 *
 * <p>搜索编排经 InMemorySearchIndex（参考语义）验证空分页、筛选、排序与
 * 分页视图组装；详情聚合验证元数据 + 字段 + 版本 + 标签 + 收藏状态与 404。</p>
 */
class AssetQueryServiceTest {

    private InMemoryAssetRepository repository;
    private AssetQueryService queryService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAssetRepository();
        InMemorySearchIndex searchIndex = new InMemorySearchIndex(repository);
        queryService = new AssetQueryServiceImpl(searchIndex, repository, org.mapstruct.factory.Mappers.getMapper(AssetAppConvertor.class));
    }

    // ---------- 搜索 ----------

    @Test
    @DisplayName("搜索返回分页视图对象：list/total/page/size 与字段映射")
    void searchReturnsPageVO() {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部"));
        repository.seed(asset("a-2", "dwd_customer_base_di", "table", "客户域", "u-2", "敏感-PII"));

        AssetSearchQuery query = new AssetSearchQuery();
        query.setCurrentUserId("u-me");
        AssetPageVO page = queryService.search(query);

        assertThat(page.getList()).hasSize(2);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(20);
        AssetVO first = page.getList().get(0);
        assertThat(first.getSource()).isEqualTo("交易中心主库");
        assertThat(first.getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("0 条命中以空分页表达（total=0，非错误）")
    void searchEmptyReturnsEmptyPage() {
        AssetSearchQuery query = new AssetSearchQuery();
        query.setKeyword("不存在");
        query.setCurrentUserId("u-me");

        AssetPageVO page = queryService.search(query);

        assertThat(page.getList()).isEmpty();
        assertThat(page.getTotal()).isZero();
        assertThat(page.getPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("关键字列级命中：命中字段名称返回资产行")
    void searchKeywordColumnLevelHit() {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部"));
        repository.seedColumns("a-1", Collections.singletonList(
                AssetColumn.builder().id("c-1").name("customer_phone").type("varchar(20)")
                        .comment("客户手机号").pk(Boolean.FALSE).classification("敏感-PII").build()));

        AssetSearchQuery query = new AssetSearchQuery();
        query.setKeyword("customer_phone");
        query.setCurrentUserId("u-me");
        AssetPageVO page = queryService.search(query);

        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getId()).isEqualTo("a-1");
    }

    @Test
    @DisplayName("筛选：favorite/mine 基于当前用户生效")
    void searchFiltersByFavoriteAndMine() {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "a_trade", "table", "交易域", "u-me", "内部"));
        repository.seed(asset("a-2", "b_cust", "table", "客户域", "u-2", "内部"));
        repository.seedFavorite("a-1", "u-me");

        AssetSearchQuery mineQuery = new AssetSearchQuery();
        mineQuery.setMine(Boolean.TRUE);
        mineQuery.setCurrentUserId("u-me");
        assertThat(queryService.search(mineQuery).getList()).extracting(AssetVO::getId)
                .containsExactly("a-1");

        AssetSearchQuery favQuery = new AssetSearchQuery();
        favQuery.setFavorite(Boolean.TRUE);
        favQuery.setCurrentUserId("u-me");
        assertThat(queryService.search(favQuery).getList()).extracting(AssetVO::getId)
                .containsExactly("a-1");
    }

    @Test
    @DisplayName("排序：sort=name 升序返回")
    void searchSortByName() {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "c_table", "table", "交易域", null, "内部"));
        repository.seed(asset("a-2", "a_table", "table", "交易域", null, "内部"));
        repository.seed(asset("a-3", "b_table", "table", "交易域", null, "内部"));

        AssetSearchQuery query = new AssetSearchQuery();
        query.setSort("name");
        query.setCurrentUserId("u-me");
        List<AssetVO> list = queryService.search(query).getList();

        assertThat(list).extracting(AssetVO::getName).containsExactly("a_table", "b_table", "c_table");
    }

    @Test
    @DisplayName("分页：page=2 返回第二页数据")
    void searchPagination() {
        repository.seedSourceName("s-1", "交易中心主库");
        for (int i = 1; i <= 5; i++) {
            repository.seed(asset("a-" + i, "t" + i, "table", "交易域", null, "内部"));
        }

        AssetSearchQuery query = new AssetSearchQuery();
        query.setPage(2);
        query.setSize(2);
        query.setCurrentUserId("u-me");
        AssetPageVO page = queryService.search(query);

        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getList()).hasSize(2);
    }

    // ---------- 详情 ----------

    @Test
    @DisplayName("详情聚合：元数据 + 字段清单 + 版本 + 标签 + 收藏状态")
    void getDetailAggregatesAll() {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部"));
        repository.seedColumns("a-1", Arrays.asList(
                AssetColumn.builder().id("c-1").name("order_id").type("bigint").comment("订单ID")
                        .pk(Boolean.TRUE).classification("内部").build(),
                AssetColumn.builder().id("c-2").name("amount").type("decimal(18,2)").comment("金额")
                        .pk(Boolean.FALSE).classification("内部").build()));
        repository.seedVersions("a-1", Collections.singletonList(
                AssetVersion.builder().id("v-1").version(2).schemaDiff("新增列 customer_phone").build()));
        repository.seedTags("a-1", Arrays.asList("核心表", "日增量"));
        repository.seedFavorite("a-1", "u-me");

        AssetDetailVO detail = queryService.getDetail("a-1", "u-me");

        assertThat(detail.getId()).isEqualTo("a-1");
        assertThat(detail.getName()).isEqualTo("dwd_trade_order_di");
        assertThat(detail.getSource()).isEqualTo("交易中心主库");
        assertThat(detail.getStatus()).isEqualTo("pending");
        assertThat(detail.getFavorite()).isTrue();
        assertThat(detail.getTags()).containsExactlyInAnyOrder("核心表", "日增量");
        assertThat(detail.getColumns()).hasSize(2);
        assertThat(detail.getColumns().get(0).getName()).isEqualTo("order_id");
        assertThat(detail.getVersions()).hasSize(1);
        assertThat(detail.getVersions().get(0).getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("详情资产不存在抛未找到（404 语义）")
    void getDetailNotFoundThrows() {
        assertThatThrownBy(() -> queryService.getDetail("not-exist", "u-me"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    @DisplayName("详情无字段/版本/标签时返回空集合而非 null")
    void getDetailEmptyPartsAreEmptyCollections() {
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部"));

        AssetDetailVO detail = queryService.getDetail("a-1", "u-me");

        assertThat(detail.getColumns()).isEmpty();
        assertThat(detail.getVersions()).isEmpty();
        assertThat(detail.getTags()).isEmpty();
        assertThat(detail.getFavorite()).isFalse();
    }

    private Asset asset(String id, String name, String type, String domain,
                        String owner, String classification) {
        return Asset.builder()
                .id(id)
                .sourceId("s-1")
                .sourceName("交易中心主库")
                .name(name)
                .type(type)
                .domain(domain)
                .owner(owner)
                .classification(classification)
                .status(AssetStatus.PENDING)
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 12))
                .build();
    }
}
