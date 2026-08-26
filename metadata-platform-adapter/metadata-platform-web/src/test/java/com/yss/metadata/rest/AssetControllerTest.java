package com.yss.metadata.rest;

import com.yss.metadata.application.asset.service.AssetActionService;
import com.yss.metadata.application.asset.service.AssetQueryService;
import com.yss.metadata.application.asset.service.convertor.AssetAppConvertor;
import com.yss.metadata.application.asset.service.impl.AssetActionServiceImpl;
import com.yss.metadata.application.asset.service.impl.AssetQueryServiceImpl;
import com.yss.metadata.application.dq.TaintStatusApplicationService;
import com.yss.metadata.domain.asset.model.Asset;

import com.yss.metadata.domain.asset.model.AssetColumn;
import com.yss.metadata.domain.asset.model.AssetStatus;
import com.yss.metadata.domain.asset.model.AssetVersion;
import com.yss.metadata.rest.advice.MetadataGlobalExceptionHandler;
import com.yss.metadata.application.asset.support.InMemoryAssetRepository;
import com.yss.metadata.application.asset.support.InMemorySearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资产 REST 契约测试（WU-02-04，冻结 OpenAPI assets 段）。
 *
 * <p>覆盖：搜索（空分页/列级命中/排序/筛选/分页/当前用户头 seam）、
 * 详情聚合 404、收藏幂等切换、认领冲突 409、标签覆盖式更新（归档 409）、
 * 归档-取消归档状态机、非法排序参数 422。</p>
 */
class AssetControllerTest {

    private static final String ASSETS_PATH = "/api/assets";

    private InMemoryAssetRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAssetRepository();
        InMemorySearchIndex searchIndex = new InMemorySearchIndex(repository);
        AssetAppConvertor convertor = org.mapstruct.factory.Mappers.getMapper(AssetAppConvertor.class);
        AssetQueryService queryService =
                new AssetQueryServiceImpl(searchIndex, repository, convertor);
        AssetActionService actionService =
                new AssetActionServiceImpl(repository, convertor);
        TaintStatusApplicationService taintStatusService =
                new TaintStatusApplicationService((assetId, taintStatus, reason, operator) -> {});
        mockMvc = MockMvcBuilders.standaloneSetup(new AssetController(queryService, actionService, taintStatusService))
                .setControllerAdvice(new MetadataGlobalExceptionHandler())
                .build();
    }


    // ---------- GET /api/assets ----------

    @Test
    @DisplayName("搜索返回 200 与 PageResult 包装（success/code/data/totalCount/pageSize/pageIndex）")
    void searchReturns200WithPageResult() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(get(ASSETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("DM-A0001"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.pageIndex").value(1))
                .andExpect(jsonPath("$.data[0].name").value("dwd_trade_order_di"))
                .andExpect(jsonPath("$.data[0].type").value("table"))
                .andExpect(jsonPath("$.data[0].source").value("交易中心主库"))
                .andExpect(jsonPath("$.data[0].status").value("claimed"))
                .andExpect(jsonPath("$.data[0].favorite").value(false));
    }

    @Test
    @DisplayName("0 条命中以空分页表达（200 + totalCount=0，非错误）")
    void searchEmptyReturnsEmptyPage() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(get(ASSETS_PATH).param("keyword", "不存在的关键词"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    @DisplayName("切片 06 RBAC：X-User-Domains 头限定资产列表（缺省全部放行，不泄露受限域）")
    void searchFilteredByAllowedDomainsHeader() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "t1", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));
        repository.seed(asset("a-2", "t2", "table", "客户域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 9, 9, 12)));
        repository.seed(asset("a-3", "t3", "table", "财务域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 8, 9, 12)));

        // 限定数据域：仅交易域 + 客户域
        mockMvc.perform(get(ASSETS_PATH).header("X-User-Domains", "交易域,客户域"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 缺省：全部放行
        mockMvc.perform(get(ASSETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    @DisplayName("keyword 列级命中：命中字段名称返回资产行")
    void searchColumnLevelHit() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));
        repository.seedColumns("a-1", Collections.singletonList(
                AssetColumn.builder().id("c-1").name("customer_phone").type("varchar(20)")
                        .comment("客户手机号").pk(Boolean.FALSE).classification("敏感-PII").build()));

        mockMvc.perform(get(ASSETS_PATH).param("keyword", "customer_phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].id").value("a-1"));
    }

    @Test
    @DisplayName("sort=name 升序返回")
    void searchSortByName() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "c_table", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));
        repository.seed(asset("a-2", "a_table", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 8, 9, 12)));

        mockMvc.perform(get(ASSETS_PATH).param("sort", "name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("a_table"))
                .andExpect(jsonPath("$.data[1].name").value("c_table"));
    }

    @Test
    @DisplayName("筛选 favorite/mine 基于请求头 X-User-Id（缺省 default-user seam）")
    void searchFavoriteAndMineWithCurrentUserHeader() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "t1", "table", "交易域", "u-me", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12)));
        repository.seed(asset("a-2", "t2", "table", "交易域", "u-other", "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 8, 0)));
        repository.seed(asset("a-3", "t3", "table", "交易域", "default-user", "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 9, 8, 0)));
        repository.seedFavorite("a-1", "u-me");

        // favorite=true + X-User-Id
        mockMvc.perform(get(ASSETS_PATH).param("favorite", "true").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].id").value("a-1"))
                .andExpect(jsonPath("$.data[0].favorite").value(true));

        // mine=true + X-User-Id
        mockMvc.perform(get(ASSETS_PATH).param("mine", "true").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].id").value("a-1"));

        // mine=true 无请求头：缺省 default-user
        mockMvc.perform(get(ASSETS_PATH).param("mine", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].id").value("a-3"));
    }

    @Test
    @DisplayName("分页参数生效：page/size 传入并回显")
    void searchPaginationParams() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        for (int i = 1; i <= 3; i++) {
            repository.seed(asset("a-" + i, "t" + i, "table", "交易域", null, "内部",
                    AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, i, 0)));
        }

        mockMvc.perform(get(ASSETS_PATH).param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.pageIndex").value(2))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("非法 sort 参数返回 422 错误体（asset.param.invalid）")
    void searchInvalidSortReturns422() throws Exception {
        mockMvc.perform(get(ASSETS_PATH).param("sort", "unknown"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("asset.param.invalid"))
                .andExpect(jsonPath("$.severity").value("error"));
    }

    // ---------- GET /api/assets/{id} ----------

    @Test
    @DisplayName("详情聚合返回 200：元数据 + 字段 + 版本 + 标签 + 收藏状态")
    void detailReturns200WithAggregate() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        Asset asset1 = asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12));
        asset1.setDatasourceType("MySQL");
        asset1.setDatabaseName("dataphin01");
        asset1.setSourceSystem("元数据采集系统demo");
        asset1.setCollectorName("MySQL采集demo");
        asset1.setRowCount(147657L);
        asset1.setStorageSize("12.03MB");
        repository.seed(asset1);
        repository.seedColumns("a-1", Arrays.asList(
                AssetColumn.builder().id("c-1").name("order_id").type("bigint").comment("订单ID")
                        .pk(Boolean.TRUE).ordinalPosition(1).classification("内部").build()));
        repository.seedVersions("a-1", Collections.singletonList(
                AssetVersion.builder().id("v-1").version(2).schemaDiff("新增列 customer_phone")
                        .createdAt(LocalDateTime.of(2026, 8, 10, 9, 12)).build()));
        repository.seedTags("a-1", Arrays.asList("核心表", "日增量"));
        repository.seedFavorite("a-1", "u-me");

        mockMvc.perform(get(ASSETS_PATH + "/a-1").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("dwd_trade_order_di"))
                .andExpect(jsonPath("$.data.source").value("交易中心主库"))
                .andExpect(jsonPath("$.data.datasourceType").value("MySQL"))
                .andExpect(jsonPath("$.data.databaseName").value("dataphin01"))
                .andExpect(jsonPath("$.data.sourceSystem").value("元数据采集系统demo"))
                .andExpect(jsonPath("$.data.collectorName").value("MySQL采集demo"))
                .andExpect(jsonPath("$.data.rowCount").value(147657))
                .andExpect(jsonPath("$.data.storageSize").value("12.03MB"))
                .andExpect(jsonPath("$.data.status").value("claimed"))
                .andExpect(jsonPath("$.data.favorite").value(true))
                .andExpect(jsonPath("$.data.tags", hasSize(2)))
                .andExpect(jsonPath("$.data.columns", hasSize(1)))
                .andExpect(jsonPath("$.data.columns[0].name").value("order_id"))
                .andExpect(jsonPath("$.data.columns[0].ordinalPosition").value(1))
                .andExpect(jsonPath("$.data.versions", hasSize(1)))
                .andExpect(jsonPath("$.data.versions[0].version").value(2));
    }

    @Test
    @DisplayName("详情不存在返回 404 错误体（asset.not_found）")
    void detailNotFoundReturns404() throws Exception {
        mockMvc.perform(get(ASSETS_PATH + "/not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    // ---------- POST /api/assets/{id}/favorite ----------

    @Test
    @DisplayName("收藏幂等切换：首次收藏 favorite=true，再次调用取消收藏 favorite=false")
    void favoriteTogglesIdempotently() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/favorite").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/favorite").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(false));
    }

    @Test
    @DisplayName("收藏不存在的资产返回 404")
    void favoriteNotFoundReturns404() throws Exception {
        mockMvc.perform(post(ASSETS_PATH + "/not-exist/favorite").header("X-User-Id", "u-me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("asset.not_found"));
    }

    // ---------- POST /api/assets/{id}/claim ----------

    @Test
    @DisplayName("认领成功：owner 置为当前用户，状态流转为已认领")
    void claimSuccess() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/claim").header("X-User-Id", "u-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value("u-me"))
                .andExpect(jsonPath("$.data.status").value("claimed"));
    }

    @Test
    @DisplayName("已被他人认领时认领返回 409 错误体（asset.claim_conflict）")
    void claimConflictReturns409() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-other", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/claim").header("X-User-Id", "u-me"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("asset.claim_conflict"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.severity").value("error"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    // ---------- PUT /api/assets/{id}/tags ----------

    @Test
    @DisplayName("标签覆盖式更新：返回 200，旧标签被全量替换")
    void updateTagsReplacesAll() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.of(2026, 8, 10, 9, 12)));
        repository.seedTags("a-1", Arrays.asList("核心表", "日增量"));

        mockMvc.perform(put(ASSETS_PATH + "/a-1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"核心表\",\"重要\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("a-1"));

        assertThat(repository.findTags("a-1")).containsExactlyInAnyOrder("核心表", "重要");
        assertThat(repository.findTags("a-1")).doesNotContain("日增量");
    }

    @Test
    @DisplayName("归档资产编辑标签返回 409 状态冲突（归档只读）")
    void updateTagsOnArchivedReturns409() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.ARCHIVED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(put(ASSETS_PATH + "/a-1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"核心表\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("asset.state_conflict"))
                .andExpect(jsonPath("$.severity").value("error"));
    }

    // ---------- POST /api/assets/{id}/archive / unarchive ----------

    @Test
    @DisplayName("归档成功 → 重复归档 409 → 取消归档恢复已认领")
    void archiveThenUnarchive() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("archived"));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/archive"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("asset.state_conflict"));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/unarchive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("claimed"));
    }

    @Test
    @DisplayName("已删除资产归档返回 409 状态冲突")
    void archiveDeletedReturns409() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                AssetStatus.DELETED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(post(ASSETS_PATH + "/a-1/archive"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("asset.state_conflict"));
    }

    @Test
    @DisplayName("PUT /api/assets/{id}/taint-status 更新存疑状态返回 200 与资产详情")
    void updateTaintStatusReturns200() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        repository.seed(asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12)));

        mockMvc.perform(put(ASSETS_PATH + "/a-1/taint-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taintStatus\":\"TAINTED\",\"reason\":\"上游根因污染\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("a-1"));
    }

    @Test
    @DisplayName("PUT /api/assets/{id}/exclude 标记资产已剔除并从默认列表中隐藏")
    void excludeAssetHidesFromDefaultList() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        Asset a1 = asset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                AssetStatus.CLAIMED, LocalDateTime.of(2026, 8, 10, 9, 12));
        a1.setDatabaseName("trade_db");
        a1.setSourceSystem("交易系统");
        repository.seed(a1);

        // 1. 默认可查出
        mockMvc.perform(get(ASSETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));

        // 2. 剔除
        mockMvc.perform(put(ASSETS_PATH + "/a-1/exclude"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isExcluded").value(true));

        // 3. 默认查询已隐藏
        mockMvc.perform(get(ASSETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 4. 查询已删除/已剔除列表可查出
        mockMvc.perform(get(ASSETS_PATH).param("isExcluded", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].name").value("dwd_trade_order_di"));

        // 5. 恢复
        mockMvc.perform(put(ASSETS_PATH + "/a-1/recover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isExcluded").value(false));

        // 6. 再次在默认列表中可见
        mockMvc.perform(get(ASSETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/assets 支持按 database 和 sourceSystem 参数精准检索")
    void queryByDatabaseAndSourceSystem() throws Exception {
        repository.seedSourceName("s-1", "交易中心主库");
        Asset a1 = asset("a-1", "order_tbl", "table", "交易域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.now());
        a1.setDatabaseName("db_trade");
        a1.setSourceSystem("核心系统");
        repository.seed(a1);

        Asset a2 = asset("a-2", "risk_tbl", "table", "风控域", null, "内部",
                AssetStatus.PENDING, LocalDateTime.now());
        a2.setDatabaseName("db_risk");
        a2.setSourceSystem("风控系统");
        repository.seed(a2);

        mockMvc.perform(get(ASSETS_PATH).param("database", "db_trade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].name").value("order_tbl"));

        mockMvc.perform(get(ASSETS_PATH).param("sourceSystem", "风控系统"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].name").value("risk_tbl"));
    }


    // ---------- 辅助 ----------

    private Asset asset(String id, String name, String type, String domain, String owner,
                        String classification, AssetStatus status, LocalDateTime updatedAt) {
        return Asset.builder()
                .id(id)
                .sourceId("s-1")
                .sourceName("交易中心主库")
                .name(name)
                .type(type)
                .domain(domain)
                .owner(owner)
                .classification(classification)
                .status(status)
                .updatedAt(updatedAt)
                .build();
    }
}
