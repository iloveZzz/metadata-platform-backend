package com.yss.metadata.infrastructure.collector;

import com.yss.datamiddleds.client.dto.datasource.ConnectionTestVO;
import com.yss.datamiddleds.client.dto.metadata.*;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.infrastructure.collector.convertor.MetadataCollectorConvertor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 远端元数据转换器单元测试。
 */
class MetadataCollectorConvertorTest {

    private final MetadataCollectorConvertor convertor = Mappers.getMapper(MetadataCollectorConvertor.class);

    @Test
    @DisplayName("TableDetailVO 转换为 CollectedAsset：表名、模式名、类型与字段/约束聚合映射正确")
    void toCollectedAsset_FullMapping() {
        TableSummaryVO summary = new TableSummaryVO();
        summary.setCatalogName("cat_demo");
        summary.setSchemaName("ods");
        summary.setTableName("t_trade_order");
        summary.setTableType("TABLE");
        summary.setTableComment("交易订单表");
        summary.setEstimatedRows(147657L);
        summary.setDataLengthBytes(10485760L);
        summary.setIndexLengthBytes(2129920L);

        ColumnVO colId = new ColumnVO();
        colId.setColumnName("id");
        colId.setRawType("bigint(20) unsigned");
        colId.setDataType("BIGINT");
        colId.setPrimaryKey(true);
        colId.setColumnComment("主键 ID");

        ColumnVO colAmount = new ColumnVO();
        colAmount.setColumnName("amount");
        colAmount.setDataType("DECIMAL");
        colAmount.setNumericPrecision(18);
        colAmount.setNumericScale(4);
        colAmount.setNullable(false);
        colAmount.setColumnComment("交易金额");

        PrimaryKeyVO pk = new PrimaryKeyVO();
        pk.setPkName("PK_t_trade_order");
        pk.setColumnNames(Collections.singletonList("id"));

        TableConstraintsVO constraints = new TableConstraintsVO();
        constraints.setPrimaryKey(pk);

        TableDetailVO detail = new TableDetailVO();
        detail.setTableMetadata(summary);
        detail.setColumns(Arrays.asList(colId, colAmount));
        detail.setConstraints(constraints);

        CollectedAsset asset = convertor.toCollectedAsset(detail);

        assertThat(asset).isNotNull();
        assertThat(asset.getName()).isEqualTo("ods.t_trade_order");
        assertThat(asset.getType()).isEqualTo("table");
        assertThat(asset.getDescription()).isEqualTo("交易订单表");
        assertThat(asset.getRowCount()).isEqualTo(147657L);
        assertThat(asset.getStorageSize()).isEqualTo("12.03MB");
        assertThat(asset.getColumns()).hasSize(2);

        CollectedColumn c1 = asset.getColumns().get(0);
        assertThat(c1.getName()).isEqualTo("id");
        assertThat(c1.getType()).isEqualTo("bigint(20) unsigned");
        assertThat(c1.getPk()).isTrue();
        assertThat(c1.getOrdinalPosition()).isEqualTo(1);
        assertThat(c1.getComment()).isEqualTo("主键 ID");

        CollectedColumn c2 = asset.getColumns().get(1);
        assertThat(c2.getName()).isEqualTo("amount");
        assertThat(c2.getType()).isEqualTo("DECIMAL(18,4)");
        assertThat(c2.getPk()).isFalse();
        assertThat(c2.getOrdinalPosition()).isEqualTo(2);
        assertThat(c2.getComment()).isEqualTo("交易金额");
    }

    @Test
    @DisplayName("连通性错误分类映射：AUTH -> CREDENTIAL, TIMEOUT/NETWORK -> NETWORK, DIALECT -> DIALECT")
    void toConnectTestResult_ErrorCategoryMapping() {
        ConnectionTestVO authFail = new ConnectionTestVO();
        authFail.setSuccess(false);
        authFail.setErrorCategory("AUTH");
        authFail.setMessage("密码错误");

        ConnectTestResult res1 = convertor.toConnectTestResult(authFail);
        assertThat(res1.isConnected()).isFalse();
        assertThat(res1.getErrorType()).isEqualTo(ConnectErrorType.CREDENTIAL);

        ConnectionTestVO timeoutFail = new ConnectionTestVO();
        timeoutFail.setSuccess(false);
        timeoutFail.setErrorCategory("TIMEOUT");
        timeoutFail.setMessage("TCP 连接超时");

        ConnectTestResult res2 = convertor.toConnectTestResult(timeoutFail);
        assertThat(res2.isConnected()).isFalse();
        assertThat(res2.getErrorType()).isEqualTo(ConnectErrorType.NETWORK);

        ConnectionTestVO dialectFail = new ConnectionTestVO();
        dialectFail.setSuccess(false);
        dialectFail.setErrorCategory("DIALECT");
        dialectFail.setMessage("GaussDB 驱动方言不支持");

        ConnectTestResult res3 = convertor.toConnectTestResult(dialectFail);
        assertThat(res3.isConnected()).isFalse();
        assertThat(res3.getErrorType()).isEqualTo(ConnectErrorType.DIALECT);
    }
}
