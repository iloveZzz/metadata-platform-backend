package com.yss.metadata.repository;

import com.yss.metadata.client.dto.query.AssetSearchQuery;
import com.yss.metadata.domain.asset.model.Asset;
import com.yss.metadata.domain.asset.model.AssetSearchResult;
import com.yss.metadata.infrastructure.convertor.AssetDirectoryConvertor;
import com.yss.metadata.repository.entity.AssetColumnPO;
import com.yss.metadata.repository.entity.AssetFavoritePO;
import com.yss.metadata.repository.entity.AssetPO;
import com.yss.metadata.repository.entity.CollectorTaskPO;
import com.yss.metadata.repository.entity.ConnectorPO;
import com.yss.metadata.client.vo.DataSourceSystemVO;
import com.yss.metadata.repository.gateway.impl.AssetSearchIndexImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资产发现索引持久化集成测试（WU-02-01，H2 内存库替代真实 MySQL）。
 *
 * <p>验证关系库 LIKE 列级命中（keyword 命中 asset.name / asset_column.name）、
 * source/type/domain/classification/favorite/mine 筛选、sort 枚举
 * （默认 updatedAt 倒序 / name / classification）、分页与 0 条空分页。</p>
 */
class AssetSearchIndexImplH2Test extends H2MapperTestSupport {

    private AssetSearchIndexImpl searchIndex;

    @BeforeEach
    void setUp() {
        searchIndex = new AssetSearchIndexImpl(
                sqlSession.getMapper(AssetRepository.class),
                sqlSession.getMapper(AssetColumnRepository.class),
                sqlSession.getMapper(AssetFavoriteRepository.class),
                sqlSession.getMapper(ConnectorRepository.class),
                sqlSession.getMapper(CollectorTaskRepository.class),
                Mappers.getMapper(AssetDirectoryConvertor.class));
    }

    @Test
    @DisplayName("keyword 命中资产名称返回资产行（含数据源名称与收藏状态组合字段）")
    void keywordMatchesAssetName() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "dwd_trade_order_di", "table", "交易域", "u-1", "内部",
                LocalDateTime.of(2026, 8, 10, 9, 12));
        seedFavorite("a-1", "u-me");

        AssetSearchQuery query = query();
        query.setKeyword("trade");
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        Asset hit = result.getItems().get(0);
        assertThat(hit.getName()).isEqualTo("dwd_trade_order_di");
        assertThat(hit.getSourceName()).isEqualTo("交易中心主库");
        assertThat(hit.getFavorite()).isTrue();
    }

    @Test
    @DisplayName("keyword 列级命中：命中字段名称返回所属资产行")
    void keywordColumnLevelHit() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                LocalDateTime.of(2026, 8, 10, 9, 12));
        seedColumn("c-1", "a-1", "customer_phone", "varchar(20)", "客户手机号", Boolean.FALSE, "敏感-PII");
        seedAsset("a-2", "dim_date", "table", "公共域", null, "内部",
                LocalDateTime.of(2026, 8, 9, 18, 0));

        AssetSearchQuery query = query();
        query.setKeyword("customer_phone");
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo("a-1");
    }

    @Test
    @DisplayName("keyword 同时命中名称与字段：资产行去重只返回一次")
    void keywordHitNameAndColumnDeduplicated() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "dwd_customer_di", "table", "交易域", null, "内部",
                LocalDateTime.of(2026, 8, 10, 9, 12));
        seedColumn("c-1", "a-1", "customer_id", "bigint", "客户ID", Boolean.FALSE, "内部");

        AssetSearchQuery query = query();
        query.setKeyword("customer");
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("0 条命中以空分页表达（total=0，非错误）")
    void emptyResultIsEmptyPage() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "dwd_trade_order_di", "table", "交易域", null, "内部",
                LocalDateTime.of(2026, 8, 10, 9, 12));

        AssetSearchQuery query = query();
        query.setKeyword("不存在的关键词");
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getPageIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("默认排序按更新时间倒序")
    void defaultSortByUpdatedAtDesc() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "t1", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 8, 0, 0));
        seedAsset("a-2", "t2", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 10, 0, 0));
        seedAsset("a-3", "t3", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 9, 0, 0));

        AssetSearchResult result = searchIndex.search(query());

        assertThat(result.getItems()).extracting(Asset::getId).containsExactly("a-2", "a-3", "a-1");
    }

    @Test
    @DisplayName("sort=name 按名称升序")
    void sortByNameAsc() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "c_table", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 10, 0, 0));
        seedAsset("a-2", "a_table", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 8, 0, 0));
        seedAsset("a-3", "b_table", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 9, 0, 0));

        AssetSearchQuery query = query();
        query.setSort("name");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getItems()).extracting(Asset::getName)
                .containsExactly("a_table", "b_table", "c_table");
    }

    @Test
    @DisplayName("sort=classification 按分级分类升序")
    void sortByClassificationAsc() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "t1", "table", "交易域", null, "受限", LocalDateTime.of(2026, 8, 10, 0, 0));
        seedAsset("a-2", "t2", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 8, 0, 0));
        seedAsset("a-3", "t3", "table", "交易域", null, "敏感-PII", LocalDateTime.of(2026, 8, 9, 0, 0));

        AssetSearchQuery query = query();
        query.setSort("classification");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getItems()).extracting(Asset::getClassification)
                .containsExactly("内部", "受限", "敏感-PII");
    }

    @Test
    @DisplayName("筛选：source（按数据源名称）/type/domain/classification 组合生效")
    void filterBySourceTypeDomainClassification() {
        seedSource("s-1", "交易中心主库");
        seedSource("s-2", "营销域 OB 集群");
        seedAsset("a-1", "dwd_trade", "table", "交易域", "u-1", "内部",
                LocalDateTime.of(2026, 8, 10, 9, 12));
        seedAsset("a-2", "dwd_cust", "table", "客户域", "u-2", "敏感-PII",
                LocalDateTime.of(2026, 8, 10, 8, 47));
        seedAsset("a-3", "col_phone", "column", "客户域", "u-2", "敏感-PII",
                LocalDateTime.of(2026, 8, 10, 8, 47));

        AssetSearchQuery query = query();
        query.setSource("交易中心主库");
        query.setType("table");
        query.setDomain("交易域");
        query.setClassification("内部");
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo("a-1");
    }

    @Test
    @DisplayName("筛选：favorite=true 仅返回当前用户收藏资产（按用户隔离）")
    void filterByFavoritePerUser() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "t1", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 10, 9, 12));
        seedAsset("a-2", "t2", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 10, 8, 0));
        seedFavorite("a-1", "u-me");
        seedFavorite("a-2", "u-other");

        AssetSearchQuery query = query();
        query.setFavorite(Boolean.TRUE);
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo("a-1");
        assertThat(result.getItems().get(0).getFavorite()).isTrue();
    }

    @Test
    @DisplayName("筛选：mine=true 仅返回 owner=当前用户的资产")
    void filterByMine() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "t1", "table", "交易域", "u-me", "内部", LocalDateTime.of(2026, 8, 10, 9, 12));
        seedAsset("a-2", "t2", "table", "交易域", "u-other", "内部", LocalDateTime.of(2026, 8, 10, 8, 0));

        AssetSearchQuery query = query();
        query.setMine(Boolean.TRUE);
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo("a-1");
    }

    @Test
    @DisplayName("分页：page/size 生效且 total 为全量命中数")
    void paginationWorks() {
        seedSource("s-1", "交易中心主库");
        for (int i = 1; i <= 5; i++) {
            seedAsset("a-" + i, "t" + i, "table", "交易域", null, "内部",
                    LocalDateTime.of(2026, 8, 10, i, 0));
        }

        AssetSearchQuery query = query();
        query.setPage(2);
        query.setSize(2);
        query.setCurrentUserId("u-me");
        AssetSearchResult result = searchIndex.search(query);

        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getPageIndex()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getItems()).hasSize(2);
    }

    // ---------- 种子辅助 ----------

    @Test
    @DisplayName("切片 06 RBAC：allowedDomains 过滤（X-User-Domains 头语义；缺省全部放行）")
    void allowedDomainsFilter() {
        seedSource("s-1", "交易中心主库");
        seedAsset("a-1", "t1", "table", "交易域", null, "内部", LocalDateTime.of(2026, 8, 10, 0, 0));
        seedAsset("a-2", "t2", "table", "客户域", null, "内部", LocalDateTime.of(2026, 8, 9, 0, 0));
        seedAsset("a-3", "t3", "table", "财务域", null, "内部", LocalDateTime.of(2026, 8, 8, 0, 0));

        // 限定数据域：仅返回交易域 + 客户域资产（不泄露财务域）
        AssetSearchQuery restricted = query();
        restricted.setAllowedDomains(java.util.Arrays.asList("交易域", "客户域"));
        AssetSearchResult filtered = searchIndex.search(restricted);
        assertThat(filtered.getTotal()).isEqualTo(2);
        assertThat(filtered.getItems()).extracting(Asset::getId)
                .containsExactlyInAnyOrder("a-1", "a-2");

        // 缺省（null）= 全部放行
        AssetSearchResult open = searchIndex.search(query());
        assertThat(open.getTotal()).isEqualTo(3);

        // 空清单 = 全部放行
        AssetSearchQuery emptyList = query();
        emptyList.setAllowedDomains(java.util.Collections.emptyList());
        assertThat(searchIndex.search(emptyList).getTotal()).isEqualTo(3);
    }

    @Test
    @DisplayName("支持按 database 与 sourceSystem 过滤，且自动装配采集任务信息")
    void databaseAndSourceSystemFilterWithCollectorInfo() {
        seedSource("s-10", "测试源");
        CollectorTaskPO taskPo = buildTaskPo("task-mysql-1", "s-10", "0 0 4 * * ?");
        taskPo.setName("MySQL采集demo");
        sqlSession.getMapper(CollectorTaskRepository.class).insert(taskPo);

        AssetPO asset1 = buildAssetPo("a-101", "s-10", "ds_test_record", "table", "sales", "owner1", "L1", "pending", LocalDateTime.now());
        asset1.setDatabaseName("dataphin02");
        asset1.setSourceSystem("核心交易系统");
        asset1.setCollectorTaskId("task-mysql-1");
        sqlSession.getMapper(AssetRepository.class).insert(asset1);

        AssetPO asset2 = buildAssetPo("a-102", "s-10", "leaf_alloc", "table", "sales", "owner2", "L2", "pending", LocalDateTime.now());
        asset2.setDatabaseName("other_db");
        asset2.setSourceSystem("风控系统");
        asset2.setCollectorTaskId("task-mysql-1");
        sqlSession.getMapper(AssetRepository.class).insert(asset2);

        // 按 database 过滤
        AssetSearchQuery qDb = query();
        qDb.setDatabase("dataphin02");
        AssetSearchResult resDb = searchIndex.search(qDb);
        assertThat(resDb.getTotal()).isEqualTo(1);
        assertThat(resDb.getItems().get(0).getName()).isEqualTo("ds_test_record");
        assertThat(resDb.getItems().get(0).getCollectorName()).isEqualTo("MySQL采集demo");
        assertThat(resDb.getItems().get(0).getUpdateFrequency()).isEqualTo("定时");

        // 按 sourceSystem 过滤
        AssetSearchQuery qSys = query();
        qSys.setSourceSystem("风控系统");
        AssetSearchResult resSys = searchIndex.search(qSys);
        assertThat(resSys.getTotal()).isEqualTo(1);
        assertThat(resSys.getItems().get(0).getName()).isEqualTo("leaf_alloc");
    }

    @Test
    @DisplayName("已剔除数据隔离：默认不返回已剔除数据，仅在 isExcluded=true 时返回")
    void excludedDataIsolation() {
        seedSource("s-20", "源20");
        AssetPO normalAsset = buildAssetPo("a-201", "s-20", "normal_table", "table", "sales", "owner1", "L1", "pending", LocalDateTime.now());
        normalAsset.setIsExcluded(false);
        sqlSession.getMapper(AssetRepository.class).insert(normalAsset);

        AssetPO excludedAsset = buildAssetPo("a-202", "s-20", "excluded_table", "table", "sales", "owner2", "L1", "pending", LocalDateTime.now());
        excludedAsset.setIsExcluded(true);
        sqlSession.getMapper(AssetRepository.class).insert(excludedAsset);

        // 默认查询：仅正常数据
        AssetSearchQuery qNormal = query();
        AssetSearchResult resNormal = searchIndex.search(qNormal);
        assertThat(resNormal.getItems()).extracting("name").contains("normal_table").doesNotContain("excluded_table");

        // 查询已剔除数据
        AssetSearchQuery qExcluded = query();
        qExcluded.setIsExcluded(true);
        AssetSearchResult resExcluded = searchIndex.search(qExcluded);
        assertThat(resExcluded.getItems()).extracting("name").contains("excluded_table").doesNotContain("normal_table");
    }

    @Test
    @DisplayName("ConnectorGateway 数据源网关集成：支持通过网关解析数据源名称与按 source 过滤")
    void connectorGatewaySourceResolution() {
        com.yss.metadata.domain.connector.gateway.ConnectorGateway mockGateway = org.mockito.Mockito.mock(com.yss.metadata.domain.connector.gateway.ConnectorGateway.class);
        com.yss.metadata.domain.connector.model.Connector connector = com.yss.metadata.domain.connector.model.Connector.builder()
                .id("ds-feign-2000")
                .name("阿斯达啊我的错_copy")
                .build();
        org.mockito.Mockito.when(mockGateway.findAll()).thenReturn(java.util.Collections.singletonList(connector));

        AssetSearchIndexImpl customSearchIndex = new AssetSearchIndexImpl(
                sqlSession.getMapper(AssetRepository.class),
                sqlSession.getMapper(AssetColumnRepository.class),
                sqlSession.getMapper(AssetFavoriteRepository.class),
                sqlSession.getMapper(ConnectorRepository.class),
                mockGateway,
                sqlSession.getMapper(CollectorTaskRepository.class),
                Mappers.getMapper(AssetDirectoryConvertor.class));

        AssetPO asset = buildAssetPo("a-gateway-1", "ds-feign-2000", "table_under_feign", "table", "sales", "owner1", "L1", "pending", LocalDateTime.now());
        asset.setDatabaseName("datamiddle_ds");
        sqlSession.getMapper(AssetRepository.class).insert(asset);

        // 1. 按 source 名称过滤
        AssetSearchQuery qSource = query();
        qSource.setSource("阿斯达啊我的错_copy");
        AssetSearchResult resSource = customSearchIndex.search(qSource);
        assertThat(resSource.getTotal()).isEqualTo(1);
        assertThat(resSource.getItems().get(0).getId()).isEqualTo("a-gateway-1");
        assertThat(resSource.getItems().get(0).getSourceName()).isEqualTo("阿斯达啊我的错_copy");

        // 2. 按 sourceId 过滤并自动装配 sourceName
        AssetSearchQuery qSourceId = query();
        qSourceId.setSourceId("ds-feign-2000");
        AssetSearchResult resSourceId = customSearchIndex.search(qSourceId);
        assertThat(resSourceId.getTotal()).isEqualTo(1);
        assertThat(resSourceId.getItems().get(0).getSourceName()).isEqualTo("阿斯达啊我的错_copy");

        // 3. 按系统显示名称（asdsa）过滤存储系统编码（asdsad）的资产
        AssetPO assetSys = buildAssetPo("a-sys-1", "ds-feign-2000", "table_under_sys", "table", "sales", "owner1", "L1", "pending", LocalDateTime.now());
        assetSys.setSourceSystem("asdsad");
        sqlSession.getMapper(AssetRepository.class).insert(assetSys);

        DataSourceSystemVO sysVO = DataSourceSystemVO.builder()
                .code("asdsad")
                .name("asdsa")
                .build();
        org.mockito.Mockito.when(mockGateway.getSystemCatalog()).thenReturn(java.util.Collections.singletonList(sysVO));

        AssetSearchQuery qSysName = query();
        qSysName.setSourceSystem("asdsa");
        AssetSearchResult resSys = customSearchIndex.search(qSysName);
        assertThat(resSys.getItems()).extracting("id").contains("a-sys-1");
    }

    private AssetSearchQuery query() {
        AssetSearchQuery query = new AssetSearchQuery();
        query.setCurrentUserId("u-me");
        return query;
    }

    private void seedSource(String id, String name) {
        ConnectorPO po = buildConnectorPo(id, name);
        sqlSession.getMapper(ConnectorRepository.class).insert(po);
    }

    private void seedAsset(String id, String name, String type, String domain, String owner,
                           String classification, LocalDateTime updatedAt) {
        AssetPO po = buildAssetPo(id, "s-1", name, type, domain, owner, classification,
                "pending", updatedAt);
        sqlSession.getMapper(AssetRepository.class).insert(po);
    }

    private void seedColumn(String id, String assetId, String name, String type, String comment,
                            Boolean pk, String classification) {
        AssetColumnPO po = new AssetColumnPO();
        po.setId(id);
        po.setAssetId(assetId);
        po.setName(name);
        po.setType(type);
        po.setComment(comment);
        po.setPk(pk);
        po.setClassification(classification);
        sqlSession.getMapper(AssetColumnRepository.class).insert(po);
    }

    private void seedFavorite(String assetId, String userId) {
        AssetFavoritePO po = new AssetFavoritePO();
        po.setAssetId(assetId);
        po.setUserId(userId);
        po.setCreatedAt(LocalDateTime.now());
        sqlSession.getMapper(AssetFavoriteRepository.class).insert(po);
    }
}
