package com.yss.metadata.infrastructure.collector.convertor;

import com.yss.datamiddleds.client.dto.datasource.ConnectionTestVO;
import com.yss.datamiddleds.client.dto.metadata.ColumnVO;
import com.yss.datamiddleds.client.dto.metadata.TableDetailVO;
import com.yss.datamiddleds.client.dto.metadata.TableSummaryVO;
import com.yss.metadata.domain.collector.model.CollectedAsset;
import com.yss.metadata.domain.collector.model.CollectedColumn;
import com.yss.metadata.domain.connector.model.ConnectErrorType;
import com.yss.metadata.domain.connector.model.ConnectTestResult;
import com.yss.metadata.infrastructure.config.MapStructInfraConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 远端数据源元数据 DTO/VO → 内部元数据资产领域模型转换器（防腐层 ACL）。
 *
 * <p>符合 ADR-0008 与 MapStructInfraConfig 规范：
 * 1. componentModel = "spring"
 * 2. unmappedTargetPolicy = WARN（未映射目标字段显式 ignore 或定制转换）
 * 3. 严格执行防腐层职责，不让外部 Feign DTO 泄漏至领域层与应用层。</p>
 */
@Mapper(config = MapStructInfraConfig.class)
public interface MetadataCollectorConvertor {

    /**
     * 将远端单表全量详情 TableDetailVO 转换为内部资产 CollectedAsset。
     */
    @Mapping(target = "name", source = "tableMetadata", qualifiedByName = "resolveAssetName")
    @Mapping(target = "type", source = "tableMetadata.tableType", qualifiedByName = "normalizeAssetType")
    @Mapping(target = "databaseName", source = "tableMetadata", qualifiedByName = "resolveDatabaseName")
    @Mapping(target = "schemaName", source = "tableMetadata.schemaName")
    @Mapping(target = "description", source = "tableMetadata.tableComment")
    @Mapping(target = "rowCount", source = "tableMetadata.estimatedRows")
    @Mapping(target = "storageSize", source = "tableMetadata", qualifiedByName = "formatStorageSize")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "domain", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "classification", ignore = true)
    @Mapping(target = "sourceSystem", ignore = true)
    @Mapping(target = "collectorTaskId", ignore = true)
    @Mapping(target = "columns", source = ".", qualifiedByName = "mapColumnsWithConstraints")
    CollectedAsset toCollectedAsset(TableDetailVO detailVO);

    /**
     * 批量转换表详情列表。
     */
    List<CollectedAsset> toCollectedAssetList(List<TableDetailVO> detailVOList);

    /**
     * 单列转换。
     */
    @Mapping(target = "name", source = "columnName")
    @Mapping(target = "type", source = ".", qualifiedByName = "resolveColumnType")
    @Mapping(target = "comment", source = "columnComment")
    @Mapping(target = "pk", source = "primaryKey", defaultValue = "false")
    @Mapping(target = "classification", ignore = true)
    CollectedColumn toCollectedColumn(ColumnVO columnVO);

    /**
     * 连通性测试结果转换。
     */
    default ConnectTestResult toConnectTestResult(ConnectionTestVO vo) {
        if (vo == null) {
            return ConnectTestResult.failure(ConnectErrorType.NETWORK, "远端连接测试响应为空");
        }
        if (Boolean.TRUE.equals(vo.getSuccess())) {
            return ConnectTestResult.success(vo.getMessage() != null ? vo.getMessage() : "连接成功");
        }
        ConnectErrorType errorType = mapErrorCategory(vo.getErrorCategory(), vo.getMessage());
        return ConnectTestResult.failure(errorType, vo.getMessage() != null ? vo.getMessage() : "连接测试失败");
    }

    // ==================== 定制映射转换方法 (Qualifiers) ====================

    @Named("resolveAssetName")
    default String resolveAssetName(TableSummaryVO summary) {
        if (summary == null || summary.getTableName() == null) {
            return "";
        }
        // 若 Schema 存在且非默认 public，采用 schema.tableName 规范
        if (summary.getSchemaName() != null && !summary.getSchemaName().trim().isEmpty()
                && !"public".equalsIgnoreCase(summary.getSchemaName())) {
            return summary.getSchemaName().trim() + "." + summary.getTableName().trim();
        }
        return summary.getTableName().trim();
    }

    @Named("resolveDatabaseName")
    default String resolveDatabaseName(TableSummaryVO summary) {
        if (summary == null) {
            return null;
        }
        if (summary.getCatalogName() != null && !summary.getCatalogName().trim().isEmpty()) {
            return summary.getCatalogName().trim();
        }
        return summary.getSchemaName() != null ? summary.getSchemaName().trim() : null;
    }

    @Named("normalizeAssetType")
    default String normalizeAssetType(String tableType) {
        if (tableType == null || tableType.trim().isEmpty()) {
            return "table";
        }
        String lower = tableType.trim().toLowerCase();
        if (lower.contains("view")) {
            return "view";
        }
        return "table";
    }

    @Named("formatStorageSize")
    default String formatStorageSize(TableSummaryVO summary) {
        if (summary == null) {
            return "0B";
        }
        long data = summary.getDataLengthBytes() != null ? summary.getDataLengthBytes() : 0L;
        long index = summary.getIndexLengthBytes() != null ? summary.getIndexLengthBytes() : 0L;
        long totalBytes = data + index;
        if (totalBytes <= 0) {
            return "0B";
        }
        if (totalBytes < 1024) {
            return totalBytes + "B";
        }
        double kb = totalBytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.2fKB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.2fMB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.ROOT, "%.2fGB", gb);
    }

    @Named("resolveColumnType")
    default String resolveColumnType(ColumnVO col) {
        if (col == null) {
            return "UNKNOWN";
        }
        if (col.getRawType() != null && !col.getRawType().trim().isEmpty()) {
            return col.getRawType().trim();
        }
        if (col.getDataType() == null) {
            return "VARCHAR";
        }
        String baseType = col.getDataType().trim().toUpperCase();
        if (col.getNumericPrecision() != null && col.getNumericScale() != null && col.getNumericScale() > 0) {
            return baseType + "(" + col.getNumericPrecision() + "," + col.getNumericScale() + ")";
        }
        if (col.getCharMaxLength() != null && col.getCharMaxLength() > 0 && col.getCharMaxLength() < 1000000) {
            return baseType + "(" + col.getCharMaxLength() + ")";
        }
        return baseType;
    }

    @Named("mapColumnsWithConstraints")
    default List<CollectedColumn> mapColumnsWithConstraints(TableDetailVO detailVO) {
        if (detailVO == null || detailVO.getColumns() == null) {
            return Collections.emptyList();
        }
        // 提取主键列名集合（双保险：列级 isPrimaryKey + 表级 PrimaryKeyVO）
        Set<String> pkNames = new HashSet<>();
        if (detailVO.getConstraints() != null && detailVO.getConstraints().getPrimaryKey() != null
                && detailVO.getConstraints().getPrimaryKey().getColumnNames() != null) {
            pkNames.addAll(detailVO.getConstraints().getPrimaryKey().getColumnNames());
        }

        List<ColumnVO> cols = detailVO.getColumns();
        List<CollectedColumn> result = new java.util.ArrayList<>(cols.size());
        for (int i = 0; i < cols.size(); i++) {
            ColumnVO col = cols.get(i);
            CollectedColumn target = toCollectedColumn(col);
            if (pkNames.contains(col.getColumnName()) || Boolean.TRUE.equals(col.getPrimaryKey())) {
                target.setPk(true);
            }
            target.setOrdinalPosition(i + 1);
            result.add(target);
        }
        return result;
    }

    default ConnectErrorType mapErrorCategory(String category, String message) {
        if (category == null) {
            return ConnectErrorType.NETWORK;
        }
        switch (category.toUpperCase()) {
            case "AUTH":
                return ConnectErrorType.CREDENTIAL;
            case "DIALECT":
                return ConnectErrorType.DIALECT;
            case "NETWORK":
            case "TIMEOUT":
            case "SCHEMA_NOT_FOUND":
                return ConnectErrorType.NETWORK;
            default:
                if (message != null && (message.toLowerCase().contains("dialect")
                        || message.toLowerCase().contains("gaussdb"))) {
                    return ConnectErrorType.DIALECT;
                }
                return ConnectErrorType.NETWORK;
        }
    }
}
