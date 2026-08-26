package com.yss.metadata.infrastructure.lineage.parser;

import com.yss.metadata.domain.lineage.parser.DefaultSqlLineageParser;
import com.yss.metadata.domain.lineage.parser.SqlLineageParser;
import com.yss.metadata.domain.lineage.parser.model.ColumnLineage;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
import com.yss.metadata.domain.lineage.parser.model.TableLineage;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 基于 JSqlParser AST (抽象语法树) 的高性能 SQL 字段血缘解析器实现。
 */
@Primary
@Component
public class JSqlParserLineageParserImpl implements SqlLineageParser {

    private static final String GAUSSDB_HINT = "gaussdb";
    private static final Set<String> AGGREGATE_FUNCTIONS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("SUM", "COUNT", "AVG", "MAX", "MIN", "STDDEV", "VARIANCE", "GROUP_CONCAT")));

    private final DefaultSqlLineageParser fallbackParser = new DefaultSqlLineageParser();

    @Override
    public SqlLineageResult parse(String sql) {
        return parse(sql, null);
    }

    @Override
    public SqlLineageResult parse(String sql, String dialectHint) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlLineageResult.unsupported("SQL 内容为空，无法解析");
        }
        if (dialectHint != null && GAUSSDB_HINT.equalsIgnoreCase(dialectHint.trim())) {
            return SqlLineageResult.unsupported("GaussDB 方言 SQL 暂不支持解析（seam-deferred：方言连接 PoC 未认证，明确提示不伪装）");
        }

        try {
            String cleanSql = stripComments(sql.trim());
            Statement stmt = CCJSqlParserUtil.parse(cleanSql);

            String targetTable = null;
            List<String> insertTargetColumns = new ArrayList<>();
            Select select = null;

            if (stmt instanceof CreateView) {
                CreateView createView = (CreateView) stmt;
                targetTable = cleanIdentifier(createView.getView().getFullyQualifiedName());
                select = createView.getSelect();
            } else if (stmt instanceof CreateTable) {
                CreateTable createTable = (CreateTable) stmt;
                targetTable = cleanIdentifier(createTable.getTable().getFullyQualifiedName());
                select = createTable.getSelect();
            } else if (stmt instanceof Insert) {
                Insert insert = (Insert) stmt;
                targetTable = cleanIdentifier(insert.getTable().getFullyQualifiedName());
                if (insert.getColumns() != null) {
                    for (Column col : insert.getColumns()) {
                        insertTargetColumns.add(cleanIdentifier(col.getColumnName()));
                    }
                }
                select = insert.getSelect();
            } else if (stmt instanceof Select) {
                select = (Select) stmt;
                targetTable = "QUERY_TARGET";
            }

            if (select == null) {
                return fallbackParser.parse(sql, dialectHint);
            }

            return extractLineageFromSelect(targetTable, insertTargetColumns, select);
        } catch (Throwable e) {
            return fallbackParser.parse(sql, dialectHint);
        }
    }

    private SqlLineageResult extractLineageFromSelect(String targetTable, List<String> insertTargetColumns, Select select) {
        PlainSelect plainSelect = select.getPlainSelect();
        if (plainSelect == null) {
            return fallbackParser.parse(select.toString(), null);
        }

        // 1. 提取 CTE 别名与 CTE 内部物理源表
        Set<String> cteNames = new HashSet<>();
        Map<String, Map<String, String>> cteColumnToTable = new HashMap<>();
        Set<String> physicalSourceTables = new LinkedHashSet<>();

        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                String cteName = withItem.getAlias() != null ? withItem.getAlias().getName() : withItem.toString();
                String cleanCte = cleanIdentifier(cteName).toLowerCase(Locale.ROOT);
                cteNames.add(cleanCte);

                if (withItem.getSelect() != null && withItem.getSelect().getPlainSelect() != null) {
                    PlainSelect cteSelect = withItem.getSelect().getPlainSelect();
                    Map<String, String> cteAliasToTable = new LinkedHashMap<>();
                    Set<String> ctePhysicalSources = new LinkedHashSet<>();
                    if (cteSelect.getFromItem() != null) {
                        processFromItem(cteSelect.getFromItem(), cteAliasToTable, ctePhysicalSources, cteNames);
                    }
                    if (cteSelect.getJoins() != null) {
                        for (Join join : cteSelect.getJoins()) {
                            if (join.getRightItem() != null) {
                                processFromItem(join.getRightItem(), cteAliasToTable, ctePhysicalSources, cteNames);
                            }
                        }
                    }
                    physicalSourceTables.addAll(ctePhysicalSources);

                    // 映射 CTE 输出字段到实际物理表
                    Map<String, String> colMap = new HashMap<>();
                    if (cteSelect.getSelectItems() != null) {
                        String defTable = ctePhysicalSources.size() == 1 ? ctePhysicalSources.iterator().next() : null;
                        for (SelectItem<?> item : cteSelect.getSelectItems()) {
                            Expression expr = item.getExpression();
                            String colName = null;
                            if (item.getAlias() != null) {
                                colName = cleanIdentifier(item.getAlias().getName());
                            } else if (expr instanceof Column) {
                                colName = cleanIdentifier(((Column) expr).getColumnName());
                            }
                            if (colName != null && defTable != null) {
                                colMap.put(colName.toLowerCase(Locale.ROOT), defTable);
                            }
                        }
                    }
                    cteColumnToTable.put(cleanCte, colMap);
                }
            }
        }

        // 2. 构建别名映射表 (Alias -> Physical Table)
        Map<String, String> aliasToTable = new LinkedHashMap<>();

        if (plainSelect.getFromItem() != null) {
            processFromItem(plainSelect.getFromItem(), aliasToTable, physicalSourceTables, cteNames);
        }

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() != null) {
                    processFromItem(join.getRightItem(), aliasToTable, physicalSourceTables, cteNames);
                }
            }
        }

        // 3. 构建表级血缘
        List<TableLineage> tableLineages = new ArrayList<>();
        if (targetTable != null && !targetTable.isEmpty()) {
            for (String sourceTable : physicalSourceTables) {
                tableLineages.add(new TableLineage(sourceTable, targetTable));
            }
        }

        // 4. 构建列级血缘
        List<ColumnLineage> columnLineages = new ArrayList<>();
        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();

        if (selectItems != null) {
            String defaultSourceTable = physicalSourceTables.size() == 1
                    ? physicalSourceTables.iterator().next() : null;

            for (int i = 0; i < selectItems.size(); i++) {
                SelectItem<?> item = selectItems.get(i);
                String targetColName = null;
                if (i < insertTargetColumns.size()) {
                    targetColName = insertTargetColumns.get(i);
                }

                Expression expr = item.getExpression();
                if (expr instanceof AllColumns) {
                    for (String srcTable : physicalSourceTables) {
                        columnLineages.add(new ColumnLineage(srcTable, "*", targetTable,
                                targetColName != null ? targetColName : "*", "*", "DIRECT"));
                    }
                } else if (expr instanceof AllTableColumns) {
                    AllTableColumns allTableCols = (AllTableColumns) expr;
                    String tableAlias = cleanIdentifier(allTableCols.getTable().getName());
                    String actualTable = aliasToTable.getOrDefault(tableAlias.toLowerCase(Locale.ROOT), tableAlias);
                    if (!cteNames.contains(actualTable.toLowerCase(Locale.ROOT))) {
                        columnLineages.add(new ColumnLineage(actualTable, "*", targetTable,
                                targetColName != null ? targetColName : "*", "*", "DIRECT"));
                    }
                } else if (expr != null) {
                    String exprStr = expr.toString();

                    if (targetColName == null) {
                        if (item.getAlias() != null) {
                            targetColName = cleanIdentifier(item.getAlias().getName());
                        } else if (expr instanceof Column) {
                            targetColName = cleanIdentifier(((Column) expr).getColumnName());
                        } else {
                            targetColName = exprStr;
                        }
                    }

                    String exprType = determineExpressionType(expr);
                    List<Column> referencedCols = extractColumns(expr);

                    if (referencedCols.isEmpty()) {
                        if (defaultSourceTable != null) {
                            columnLineages.add(new ColumnLineage(defaultSourceTable, targetColName,
                                    targetTable, targetColName, exprStr, exprType));
                        }
                    } else {
                        for (Column refCol : referencedCols) {
                            String colName = cleanIdentifier(refCol.getColumnName());
                            String srcTable = null;
                            if (refCol.getTable() != null && refCol.getTable().getName() != null) {
                                String tableAlias = cleanIdentifier(refCol.getTable().getName());
                                String resolved = aliasToTable.getOrDefault(tableAlias.toLowerCase(Locale.ROOT), tableAlias);
                                if (cteNames.contains(resolved.toLowerCase(Locale.ROOT))) {
                                    Map<String, String> colMap = cteColumnToTable.get(resolved.toLowerCase(Locale.ROOT));
                                    if (colMap != null && colMap.containsKey(colName.toLowerCase(Locale.ROOT))) {
                                        srcTable = colMap.get(colName.toLowerCase(Locale.ROOT));
                                    } else {
                                        srcTable = defaultSourceTable;
                                    }
                                } else {
                                    srcTable = resolved;
                                }
                            } else {
                                srcTable = defaultSourceTable;
                            }

                            if (srcTable != null) {
                                columnLineages.add(new ColumnLineage(
                                        srcTable,
                                        colName,
                                        targetTable != null ? targetTable : "QUERY_TARGET",
                                        targetColName,
                                        exprStr,
                                        exprType
                                ));
                            }
                        }
                    }
                }
            }
        }

        return SqlLineageResult.supported(tableLineages, columnLineages);
    }

    private void processFromItem(FromItem fromItem, Map<String, String> aliasToTable,
                                 Set<String> physicalSourceTables, Set<String> cteNames) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String tableName = cleanIdentifier(table.getFullyQualifiedName());
            String alias = table.getAlias() != null ? cleanIdentifier(table.getAlias().getName()) : null;

            if (alias != null) {
                aliasToTable.put(alias.toLowerCase(Locale.ROOT), tableName);
            }
            aliasToTable.put(tableName.toLowerCase(Locale.ROOT), tableName);
            if (table.getName() != null) {
                aliasToTable.put(cleanIdentifier(table.getName()).toLowerCase(Locale.ROOT), tableName);
            }

            if (!cteNames.contains(tableName.toLowerCase(Locale.ROOT))) {
                physicalSourceTables.add(tableName);
            }
        }
    }

    private String determineExpressionType(Expression expr) {
        if (expr instanceof Column) {
            return "DIRECT";
        }
        if (expr instanceof Function) {
            Function func = (Function) expr;
            String funcName = func.getName().toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(funcName)) {
                return "AGGREGATE";
            }
            return "COMPUTED";
        }
        if (expr instanceof BinaryExpression || expr instanceof CaseExpression) {
            return "COMPUTED";
        }
        return "COMPUTED";
    }

    private List<Column> extractColumns(Expression expr) {
        List<Column> columns = new ArrayList<>();
        if (expr instanceof Column) {
            columns.add((Column) expr);
        } else if (expr instanceof Function) {
            Function func = (Function) expr;
            if (func.getParameters() != null && func.getParameters().getExpressions() != null) {
                for (Expression param : func.getParameters().getExpressions()) {
                    columns.addAll(extractColumns(param));
                }
            }
        } else if (expr instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expr;
            columns.addAll(extractColumns(bin.getLeftExpression()));
            columns.addAll(extractColumns(bin.getRightExpression()));
        } else if (expr instanceof CaseExpression) {
            CaseExpression caseExpr = (CaseExpression) expr;
            if (caseExpr.getSwitchExpression() != null) {
                columns.addAll(extractColumns(caseExpr.getSwitchExpression()));
            }
            if (caseExpr.getElseExpression() != null) {
                columns.addAll(extractColumns(caseExpr.getElseExpression()));
            }
        }
        return columns;
    }

    private String cleanIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.replace("`", "").replace("\"", "").replace("[", "").replace("]", "").trim();
    }

    private String stripComments(String sql) {
        if (sql == null) {
            return "";
        }
        String noBlock = sql.replaceAll("/\\*.*?\\*/", " ");
        return noBlock.replaceAll("--.*", " ").trim();
    }
}
