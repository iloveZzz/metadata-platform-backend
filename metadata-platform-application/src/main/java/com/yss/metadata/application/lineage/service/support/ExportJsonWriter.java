package com.yss.metadata.application.lineage.service.support;

import com.yss.metadata.client.vo.ImpactGroupVO;
import com.yss.metadata.client.vo.ImpactItemVO;
import com.yss.metadata.client.vo.ImpactVO;

/**
 * 导出 JSON 渲染器（影响分析 ImpactVO → JSON 文本）。
 *
 * <p>受控实现说明：应用模块编译类路径不含 JSON 库（pom 不可改，见合同
 * allowed_write_paths），导出 JSON 采用本轻量渲染器（字段顺序确定、字符串
 * 严格转义）；仅用于导出文件内容，不承载反序列化。字符串转义覆盖
 * 双引号、反斜杠、b/f/n/r/t 与 U+0000~U+001F 控制字符（按 unicode 转义输出）。</p>
 */
public final class ExportJsonWriter {

    private ExportJsonWriter() {
    }

    /**
     * ImpactVO → JSON 文本。
     */
    public static String toJson(ImpactVO impact) {
        StringBuilder json = new StringBuilder(256);
        json.append("{\"sortBy\":").append(quote(impact.getSortBy()))
                .append(",\"groups\":[");
        boolean firstGroup = true;
        for (ImpactGroupVO group : impact.getGroups()) {
            if (!firstGroup) {
                json.append(',');
            }
            firstGroup = false;
            json.append("{\"depth\":").append(group.getDepth()).append(",\"items\":[");
            boolean firstItem = true;
            for (ImpactItemVO item : group.getItems()) {
                if (!firstItem) {
                    json.append(',');
                }
                firstItem = false;
                json.append("{\"assetId\":").append(quote(item.getAssetId()))
                        .append(",\"name\":").append(quote(item.getName()))
                        .append(",\"type\":").append(quote(item.getType()))
                        .append(",\"domain\":").append(quote(item.getDomain()))
                        .append(",\"classification\":").append(quote(item.getClassification()))
                        .append(",\"risk\":").append(quote(item.getRisk()))
                        .append(",\"depth\":").append(item.getDepth())
                        .append('}');
            }
            json.append("]}");
        }
        json.append("]}");
        return json.toString();
    }

    /**
     * JSON 字符串字面量（null → null 字面量；转义双引号/反斜杠/控制字符）。
     */
    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
