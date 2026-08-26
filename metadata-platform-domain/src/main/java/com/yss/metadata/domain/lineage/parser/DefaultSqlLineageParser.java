package com.yss.metadata.domain.lineage.parser;

import com.yss.metadata.domain.lineage.parser.model.ColumnLineage;
import com.yss.metadata.domain.lineage.parser.model.SqlLineageResult;
import com.yss.metadata.domain.lineage.parser.model.TableLineage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 血缘解析器默认实现（MySQL/OceanBase 兼容；GaussDB 方言 seam-deferred）。
 *
 * <p>解析策略（抽样准确率 ≥80% 目标，FR-010）：
 * <ul>
 *   <li>方言探测：dialectHint=gaussdb 或检测到 PG/GaussDB 语法（RETURNING /
 *       ON CONFLICT / :: 强转 / $$ 美元引号 / ILIKE）→ 明确返回不支持提示；</li>
 *   <li>目标识别：INSERT INTO / INSERT OVERWRITE (OceanBase) / CREATE TABLE|VIEW AS；</li>
 *   <li>源表抽取：词法扫描 SELECT 段 FROM/JOIN（含逗号列表、限定名、子查询、
 *       CTE 体），排除 CTE 别名与派生表；</li>
 *   <li>列级血缘：INSERT 显式列清单与 SELECT 表达式按位置映射，列引用解析到源表。</li>
 * </ul>
 * 已知近似（登记于 seam_deferred）：复杂嵌套表达式/字符串内关键字为近似处理。</p>
 */
public class DefaultSqlLineageParser implements SqlLineageParser {

    /** GaussDB 方言提示 */
    private static final String GAUSSDB_HINT = "gaussdb";

    /** 标识符字符（含 MySQL 反引号） */
    private static final String IDENT_CHARS = "[A-Za-z0-9_$`]+";

    /** 限定名（db.table） */
    private static final String QUALIFIED = IDENT_CHARS + "(?:\\s*\\.\\s*" + IDENT_CHARS + ")?";

    private static final Pattern INSERT_OVERWRITE = Pattern.compile(
            "(?is)^INSERT\\s+OVERWRITE\\s+(?:INTO\\s+|TABLE\\s+)(" + QUALIFIED + ")");

    private static final Pattern INSERT_INTO = Pattern.compile(
            "(?is)^INSERT\\s+(?:IGNORE\\s+)?INTO\\s+(" + QUALIFIED + ")\\s*(\\(([^()]*)\\))?");

    private static final Pattern CREATE_AS = Pattern.compile(
            "(?is)^CREATE\\s+(?:OR\\s+REPLACE\\s+)?(TABLE|VIEW)\\s+(" + QUALIFIED + ")\\s+AS\\s+");

    private static final Set<String> KEYWORDS = Collections.unmodifiableSet(new LinkedHashSet<>(
            java.util.Arrays.asList(
            "SELECT", "FROM", "JOIN", "WHERE", "WITH", "AS", "ON", "LEFT", "RIGHT", "INNER",
            "OUTER", "CROSS", "FULL", "GROUP", "ORDER", "BY", "HAVING", "LIMIT", "UNION",
            "ALL", "VALUES", "INTO", "INSERT", "OVERWRITE", "TABLE", "CREATE", "VIEW",
            "RECURSIVE", "DISTINCT", "AND", "OR", "NOT", "IN", "IS", "NULL", "CASE", "WHEN",
            "THEN", "ELSE", "END", "RETURNING", "CONFLICT", "ILIKE", "USE", "FORCE", "INDEX",
            "UPDATE", "SET", "DELETE", "LIKE", "BETWEEN", "EXISTS", "ASC", "DESC", "PARTITION",
            "OVER", "STRAIGHT_JOIN", "NATURAL", "USING")));

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
            return SqlLineageResult.unsupported(
                    "GaussDB 方言 SQL 暂不支持解析（seam-deferred：方言连接 PoC 未认证，明确提示不伪装）");
        }
        String cleaned = stripComments(sql);
        List<Token> tokens = tokenize(cleaned);
        if (detectPgSyntax(tokens)) {
            return SqlLineageResult.unsupported(
                    "检测到 GaussDB/PostgreSQL 语法（RETURNING / ON CONFLICT / :: 强转 / $$ 美元引号 / ILIKE），"
                            + "暂不支持解析（seam-deferred：方言连接 PoC 未认证，明确提示不伪装）");
        }
        return parseMySqlCompatible(cleaned, tokens);
    }

    // ---------- 方言探测 ----------

    private boolean detectPgSyntax(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (isKeyword(token, "RETURNING") || isKeyword(token, "ILIKE")) {
                return true;
            }
            if ("::".equals(token.text)) {
                return true;
            }
            if (isKeyword(token, "ON") && i + 1 < tokens.size()
                    && isKeyword(tokens.get(i + 1), "CONFLICT")) {
                return true;
            }
            if ("$".equals(token.text) && i + 1 < tokens.size() && "$".equals(tokens.get(i + 1).text)) {
                return true;
            }
        }
        return false;
    }

    // ---------- MySQL/OceanBase 兼容解析 ----------

    private SqlLineageResult parseMySqlCompatible(String sql, List<Token> tokens) {
        TargetStatement target = extractTarget(sql);
        if (target == null) {
            // 无目标表（SELECT / WITH ... SELECT 只读语句）：无表级血缘
            return SqlLineageResult.supported(Collections.emptyList(), Collections.emptyList());
        }
        List<Token> restTokens = tokenize(target.rest);
        Set<String> cteAliases = extractCteAliases(restTokens);
        SourceScan sourceScan = extractSourceTables(restTokens, cteAliases);

        List<TableLineage> tableLineage = new ArrayList<>();
        for (String source : sourceScan.tables) {
            tableLineage.add(new TableLineage(source, target.table));
        }

        List<ColumnLineage> columnLineage = buildColumnLineage(target, restTokens, sourceScan, cteAliases);
        return SqlLineageResult.supported(tableLineage, columnLineage);
    }

    /**
     * 目标语句解析结果（table=目标表名，columns=INSERT 显式列清单，rest=SELECT/WITH/VALUES 段）。
     */
    private TargetStatement extractTarget(String sql) {
        String s = sql.trim();
        Matcher overwrite = INSERT_OVERWRITE.matcher(s);
        if (overwrite.find()) {
            return new TargetStatement(normalizeIdent(overwrite.group(1)), null,
                    s.substring(overwrite.end()));
        }
        Matcher insert = INSERT_INTO.matcher(s);
        if (insert.find()) {
            return new TargetStatement(normalizeIdent(insert.group(1)),
                    splitColumns(insert.group(3)), s.substring(insert.end()));
        }
        Matcher create = CREATE_AS.matcher(s);
        if (create.find()) {
            return new TargetStatement(normalizeIdent(create.group(2)), null,
                    s.substring(create.end()));
        }
        return null;
    }

    private List<String> splitColumns(String columns) {
        if (columns == null || columns.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String column : columns.split(",")) {
            String trimmed = column.trim();
            if (!trimmed.isEmpty()) {
                result.add(normalizeIdent(trimmed));
            }
        }
        return result;
    }

    /**
     * CTE 别名抽取（词法：WITH 头部到首个 AS 子查询，支持多 CTE 逗号续行）。
     */
    private Set<String> extractCteAliases(List<Token> tokens) {
        Set<String> aliases = new LinkedHashSet<>();
        boolean inWithHeader = false;
        boolean inSubquery = false;
        int depth = 0;
        for (Token token : tokens) {
            if ("(".equals(token.text)) {
                if (inSubquery) {
                    depth++;
                } else if (inWithHeader) {
                    inSubquery = true;
                    depth = 1;
                } else {
                    depth++;
                }
                continue;
            }
            if (")".equals(token.text)) {
                if (inSubquery) {
                    depth--;
                    if (depth == 0) {
                        inSubquery = false;
                    }
                } else if (depth > 0) {
                    depth--;
                }
                continue;
            }
            if (inWithHeader && !inSubquery) {
                if (isKeyword(token, "SELECT") || isKeyword(token, "VALUES")
                        || isKeyword(token, "INSERT") || isKeyword(token, "UPDATE")) {
                    inWithHeader = false;
                } else if (token.kind == TokenKind.IDENT && !isKeyword(token, "RECURSIVE")) {
                    aliases.add(normalizeIdent(token.text));
                }
            }
            if (depth == 0 && isKeyword(token, "WITH") && !inWithHeader && !inSubquery) {
                inWithHeader = true;
            }
        }
        return aliases;
    }

    /**
     * 源表扫描：FROM/JOIN 后的限定名表（含逗号列表、别名、CTE 别名与派生表排除）。
     */
    private SourceScan extractSourceTables(List<Token> tokens, Set<String> cteAliases) {
        List<String> tables = new ArrayList<>();
        Map<String, String> aliasToTable = new LinkedHashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!isKeyword(token, "FROM") && !isKeyword(token, "JOIN")) {
                continue;
            }
            int j = i + 1;
            // 派生表/子查询：FROM ( ...
            if (j < tokens.size() && "(".equals(tokens.get(j).text)) {
                continue;
            }
            if (j >= tokens.size() || tokens.get(j).kind != TokenKind.IDENT) {
                continue;
            }
            String table = consumeQualified(tokens, j);
            j += 1 + (isDotTable(tokens, j) ? 2 : 0);
            if (table != null && !cteAliases.contains(table)) {
                tables.add(table);
                aliasToTable.putIfAbsent(lastSegment(table), table);
            }
            // 别名（AS alias 或直接 alias），然后处理逗号续表
            j = consumeAliasAndRegister(tokens, j, table, aliasToTable);
            while (j < tokens.size() && ",".equals(tokens.get(j).text)) {
                j++;
                if (j < tokens.size() && "(".equals(tokens.get(j).text)) {
                    break;
                }
                if (j < tokens.size() && tokens.get(j).kind == TokenKind.IDENT) {
                    String next = consumeQualified(tokens, j);
                    j += 1 + (isDotTable(tokens, j) ? 2 : 0);
                    if (next != null && !cteAliases.contains(next)) {
                        tables.add(next);
                        aliasToTable.putIfAbsent(lastSegment(next), next);
                    }
                    j = consumeAliasAndRegister(tokens, j, next, aliasToTable);
                }
            }
        }
        return new SourceScan(tables, aliasToTable);
    }

    /**
     * 消费可选别名（AS alias 或直接 alias）并登记 别名→表 映射（列级血缘解析用）。
     */
    private int consumeAliasAndRegister(List<Token> tokens, int j, String table,
                                        Map<String, String> aliasToTable) {
        if (j >= tokens.size()) {
            return j;
        }
        if (isKeyword(tokens.get(j), "AS")) {
            j++;
        }
        if (j < tokens.size() && tokens.get(j).kind == TokenKind.IDENT
                && !isKeywordText(tokens.get(j).text)) {
            if (table != null) {
                aliasToTable.put(normalizeIdent(tokens.get(j).text), table);
            }
            return j + 1;
        }
        return j;
    }

    private boolean isDotTable(List<Token> tokens, int j) {
        return j + 1 < tokens.size() && ".".equals(tokens.get(j).text)
                && j + 2 < tokens.size() && tokens.get(j + 1).kind == TokenKind.IDENT;
    }

    private String consumeQualified(List<Token> tokens, int j) {
        if (j >= tokens.size() || tokens.get(j).kind != TokenKind.IDENT) {
            return null;
        }
        String first = normalizeIdent(tokens.get(j).text);
        if (isDotTable(tokens, j)) {
            return first + "." + normalizeIdent(tokens.get(j + 2).text);
        }
        return first;
    }

    // ---------- 列级血缘 ----------

    private List<ColumnLineage> buildColumnLineage(TargetStatement target, List<Token> restTokens,
                                                   SourceScan sourceScan, Set<String> cteAliases) {
        if (target.columns.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Token>> expressions = extractSelectExpressions(restTokens);
        if (expressions.size() != target.columns.size()) {
            return Collections.emptyList();
        }
        List<ColumnLineage> result = new ArrayList<>();
        for (int i = 0; i < target.columns.size(); i++) {
            ColumnRef ref = resolveColumnRef(expressions.get(i), sourceScan, cteAliases);
            if (ref != null) {
                result.add(new ColumnLineage(ref.fromTable, ref.fromColumn,
                        target.table, target.columns.get(i)));
            }
        }
        return result;
    }

    /**
     * 顶层 SELECT 表达式抽取（深度 0 逗号分隔，止于 FROM/JOIN/WHERE/UNION 等）。
     */
    private List<List<Token>> extractSelectExpressions(List<Token> tokens) {
        int selectIndex = -1;
        int depth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("(".equals(token.text)) {
                depth++;
            } else if (")".equals(token.text)) {
                depth--;
            } else if (depth == 0 && isKeyword(token, "SELECT")) {
                selectIndex = i;
                break;
            }
        }
        if (selectIndex < 0) {
            return Collections.emptyList();
        }
        List<List<Token>> expressions = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        depth = 0;
        for (int i = selectIndex + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("(".equals(token.text)) {
                depth++;
                current.add(token);
                continue;
            }
            if (")".equals(token.text)) {
                depth--;
                current.add(token);
                continue;
            }
            if (depth == 0 && isTerminatorKeyword(token)) {
                break;
            }
            if (depth == 0 && ",".equals(token.text)) {
                if (!current.isEmpty()) {
                    expressions.add(current);
                }
                current = new ArrayList<>();
                continue;
            }
            current.add(token);
        }
        if (!current.isEmpty()) {
            expressions.add(current);
        }
        return expressions;
    }

    private boolean isTerminatorKeyword(Token token) {
        return isKeyword(token, "FROM") || isKeyword(token, "JOIN")
                || isKeyword(token, "WHERE") || isKeyword(token, "GROUP")
                || isKeyword(token, "ORDER") || isKeyword(token, "HAVING")
                || isKeyword(token, "LIMIT") || isKeyword(token, "UNION")
                || isKeyword(token, "INTO") || isKeyword(token, "VALUES")
                || isKeyword(token, "RETURNING") || isKeyword(token, "WINDOW")
                || isKeyword(token, "STARTING") || isKeyword(token, "ENDING");
    }

    /**
     * 列引用解析：expr 中的基列名 + 源表解析（限定符→表 或 单一源表兜底）。
     */
    private ColumnRef resolveColumnRef(List<Token> expression, SourceScan sourceScan, Set<String> cteAliases) {
        // 剔除尾部别名（AS alias 或末尾单标识符）
        List<Token> body = stripExpressionAlias(expression);
        if (body.isEmpty()) {
            return null;
        }
        String column = null;
        String qualifier = null;
        for (int i = 0; i < body.size(); i++) {
            Token token = body.get(i);
            if (token.kind != TokenKind.IDENT || isKeywordText(token.text)) {
                continue;
            }
            // 函数名跳过（后随左括号）
            if (i + 1 < body.size() && "(".equals(body.get(i + 1).text)) {
                continue;
            }
            // 限定列：ident . ident
            if (i + 2 < body.size() && ".".equals(body.get(i + 1).text)
                    && body.get(i + 2).kind == TokenKind.IDENT) {
                qualifier = normalizeIdent(token.text);
                column = normalizeIdent(body.get(i + 2).text);
                break;
            }
            // 普通列
            column = normalizeIdent(token.text);
            break;
        }
        if (column == null || "*".equals(column)) {
            return null;
        }
        String fromTable = resolveSourceTable(qualifier, sourceScan, cteAliases);
        if (fromTable == null) {
            return null;
        }
        return new ColumnRef(fromTable, column);
    }

    private List<Token> stripExpressionAlias(List<Token> expression) {
        List<Token> body = new ArrayList<>(expression);
        // AS alias
        for (int i = 0; i + 1 < body.size(); i++) {
            if (isKeyword(body.get(i), "AS")) {
                return new ArrayList<>(body.subList(0, i));
            }
        }
        // 末尾单标识符（非关键字）视为别名；单标识符表达式本身是列，不剥离
        int last = body.size() - 1;
        if (last > 0 && body.get(last).kind == TokenKind.IDENT
                && !isKeywordText(body.get(last).text)) {
            return new ArrayList<>(body.subList(0, last));
        }
        return body;
    }

    private String resolveSourceTable(String qualifier, SourceScan sourceScan, Set<String> cteAliases) {
        String table = null;
        if (qualifier != null) {
            table = sourceScan.aliasToTable.get(qualifier);
            if (table == null && sourceScan.tables.contains(qualifier)) {
                table = qualifier;
            }
        } else if (sourceScan.tables.size() == 1) {
            // 无限定符：单一源表兜底（不猜测多表场景）
            table = sourceScan.tables.get(0);
        }
        if (table == null) {
            return null;
        }
        // CTE 别名/派生表无法定位真实源表，跳过列级（不猜测）
        if (cteAliases.contains(table)) {
            return null;
        }
        return table;
    }

    // ---------- 词法与文本工具 ----------

    private String stripComments(String sql) {
        String withoutLine = sql.replaceAll("(?m)--[^\\r\\n]*", " ")
                .replaceAll("(?m)#[^\\r\\n]*", " ");
        return withoutLine.replaceAll("(?s)/\\*.*?\\*/", " ");
    }

    private List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = sql.length();
        while (i < n) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '\'' || c == '"') {
                int j = i + 1;
                StringBuilder sb = new StringBuilder();
                while (j < n) {
                    char cc = sql.charAt(j);
                    if (cc == c) {
                        if (j + 1 < n && sql.charAt(j + 1) == c) {
                            sb.append(c);
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    sb.append(cc);
                    j++;
                }
                i = Math.min(j + 1, n);
                tokens.add(new Token(TokenKind.STRING, sb.toString()));
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < n && (Character.isDigit(sql.charAt(j)) || sql.charAt(j) == '.')) {
                    j++;
                }
                tokens.add(new Token(TokenKind.NUMBER, sql.substring(i, j)));
                i = j;
                continue;
            }
            if (isIdentPart(c)) {
                int j = i;
                while (j < n && isIdentPart(sql.charAt(j))) {
                    j++;
                }
                tokens.add(new Token(TokenKind.IDENT, sql.substring(i, j)));
                i = j;
                continue;
            }
            String two = (i + 1 < n) ? sql.substring(i, i + 2) : "";
            if ("::".equals(two) || ">=".equals(two) || "<=".equals(two)
                    || "!=".equals(two) || "||".equals(two)) {
                tokens.add(new Token(TokenKind.PUNCT, two));
                i += 2;
                continue;
            }
            tokens.add(new Token(TokenKind.PUNCT, String.valueOf(c)));
            i++;
        }
        return tokens;
    }

    private boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '`';
    }

    private boolean isKeyword(Token token, String keyword) {
        return token.kind == TokenKind.IDENT && isKeywordText(token.text)
                && keyword.equalsIgnoreCase(token.text);
    }

    private boolean isKeywordText(String text) {
        return KEYWORDS.contains(text.toUpperCase(Locale.ROOT));
    }

    private String normalizeIdent(String ident) {
        return ident.replace("`", "").replaceAll("\\s+", "");
    }

    private String lastSegment(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }

    // ---------- 内部结构 ----------

    private enum TokenKind { IDENT, STRING, NUMBER, PUNCT }

    private static final class Token {
        final TokenKind kind;
        final String text;

        Token(TokenKind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    private static final class TargetStatement {
        final String table;
        final List<String> columns;
        final String rest;

        TargetStatement(String table, List<String> columns, String rest) {
            this.table = table;
            this.columns = columns == null ? Collections.emptyList() : columns;
            this.rest = rest;
        }
    }

    private static final class SourceScan {
        final List<String> tables;
        final Map<String, String> aliasToTable;

        SourceScan(List<String> tables, Map<String, String> aliasToTable) {
            this.tables = tables;
            this.aliasToTable = aliasToTable;
        }
    }

    private static final class ColumnRef {
        final String fromTable;
        final String fromColumn;

        ColumnRef(String fromTable, String fromColumn) {
            this.fromTable = fromTable;
            this.fromColumn = fromColumn;
        }
    }
}
