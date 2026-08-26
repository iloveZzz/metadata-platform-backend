package com.yss.datamiddle.semantic.metric.batch;

import com.yss.datamiddle.semantic.metric.model.MetricDefinition;
import com.yss.datamiddle.semantic.metric.model.MetricVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 指标口径 CSV 文本解析与导出器
 */
public class MetricCsvParser {

    public static final String CSV_HEADER = "指标名称,指标分组,业务口径,负责人,计算公式,逻辑描述";

    /**
     * 解析 CSV 字符串为指标导入条目列表
     */
    public List<MetricImportItem> parseCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] lines = csvContent.split("\\r?\\n");
        List<MetricImportItem> items = new ArrayList<>();

        int startIndex = 0;
        if (lines.length > 0 && lines[0].contains("指标名称")) {
            startIndex = 1; // 跳过表头
        }

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            List<String> tokens = parseCsvLine(line);
            if (tokens.isEmpty()) {
                continue;
            }

            String name = tokens.size() > 0 ? tokens.get(0).trim() : "";
            String group = tokens.size() > 1 ? tokens.get(1).trim() : "DEFAULT";
            String desc = tokens.size() > 2 ? tokens.get(2).trim() : "";
            String owner = tokens.size() > 3 ? tokens.get(3).trim() : "";
            String expr = tokens.size() > 4 ? tokens.get(4).trim() : "";
            String logic = tokens.size() > 5 ? tokens.get(5).trim() : "";

            items.add(MetricImportItem.builder()
                    .rowNumber(i + 1)
                    .name(name)
                    .metricGroup(group.isEmpty() ? "DEFAULT" : group)
                    .description(desc)
                    .owner(owner)
                    .expression(expr)
                    .logicDescription(logic)
                    .build());
        }

        return items;
    }

    /**
     * 将指标列表导出为标准 CSV 字符串
     */
    public String exportToCsv(List<MetricDefinition> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");

        if (metrics != null) {
            for (MetricDefinition m : metrics) {
                String name = escapeCsv(m.getName());
                String group = escapeCsv(m.getMetricGroup());
                String desc = escapeCsv(m.getDescription());
                String owner = escapeCsv(m.getOwner());

                String expr = "";
                String logic = "";
                if (m.getVersions() != null && !m.getVersions().isEmpty()) {
                    MetricVersion latest = m.getVersions().get(m.getVersions().size() - 1);
                    expr = escapeCsv(latest.getExpression());
                    logic = escapeCsv(latest.getLogicDescription());
                }

                sb.append(name).append(",")
                        .append(group).append(",")
                        .append(desc).append(",")
                        .append(owner).append(",")
                        .append(expr).append(",")
                        .append(logic).append("\n");
            }
        }

        return sb.toString();
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // 跳过转义双引号
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
