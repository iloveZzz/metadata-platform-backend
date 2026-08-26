package com.yss.datasecurity.domain.service;

import com.yss.datasecurity.domain.model.SimulationFieldMatch;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SensitiveRecognitionSimulationEngine {

    public List<SimulationFieldMatch> simulate(String datasourceId, List<String> tableNames, String fieldNameRegex, String contentSampleRegex, Double threshold) {
        List<SimulationFieldMatch> results = new ArrayList<>();
        if (tableNames == null || tableNames.isEmpty()) {
            return results;
        }

        Pattern fieldPattern = null;
        if (fieldNameRegex != null && !fieldNameRegex.trim().isEmpty()) {
            try {
                fieldPattern = Pattern.compile(fieldNameRegex, Pattern.CASE_INSENSITIVE);
            } catch (Exception ignored) {
            }
        }

        Pattern contentPattern = null;
        if (contentSampleRegex != null && !contentSampleRegex.trim().isEmpty()) {
            try {
                contentPattern = Pattern.compile(contentSampleRegex);
            } catch (Exception ignored) {
            }
        }

        // 为指定表生成模拟采样字段与匹配分析（至多10张表）
        int maxTables = Math.min(tableNames.size(), 10);
        for (int i = 0; i < maxTables; i++) {
            String table = tableNames.get(i);
            List<MockField> mockFields = generateMockFieldsForTable(table);

            for (MockField field : mockFields) {
                boolean matched = false;
                String matchedCondition = "";

                if (fieldPattern != null && fieldPattern.matcher(field.name).find()) {
                    matched = true;
                    matchedCondition = "字段名正则命中: " + fieldNameRegex;
                } else if (contentPattern != null && contentPattern.matcher(field.sampleValue).find()) {
                    matched = true;
                    matchedCondition = "内容特征采样正则命中 (阈值 " + (threshold != null ? (int)(threshold * 100) : 80) + "%): " + contentSampleRegex;
                } else if (field.name.toLowerCase().contains("phone") || field.name.toLowerCase().contains("mobile")) {
                    matched = true;
                    matchedCondition = "语义特征智能判定命中 (手机号)";
                } else if (field.name.toLowerCase().contains("id_card") || field.name.toLowerCase().contains("cert_no")) {
                    matched = true;
                    matchedCondition = "语义特征智能判定命中 (居民身份证号)";
                }

                if (matched) {
                    results.add(SimulationFieldMatch.builder()
                        .tableName(table)
                        .fieldName(field.name)
                        .sampleValue(field.sampleValue)
                        .matchedCategoryName(field.categoryName)
                        .securityGradeName(field.gradeName)
                        .matchedCondition(matchedCondition)
                        .build());
                }
            }
        }

        return results;
    }

    private List<MockField> generateMockFieldsForTable(String tableName) {
        List<MockField> list = new ArrayList<>();
        if (tableName.toLowerCase().contains("user") || tableName.toLowerCase().contains("cust") || tableName.toLowerCase().contains("member")) {
            list.add(new MockField("mobile_phone", "13800138000", "个人手机号码", "L3 敏感机密"));
            list.add(new MockField("cert_no", "110101199003072345", "居民身份证号", "L4 绝密高危"));
            list.add(new MockField("cust_name", "张三", "个人姓名", "L2 对内公开"));
            list.add(new MockField("email_addr", "zhangsan@example.com", "电子邮箱", "L2 对内公开"));
        } else if (tableName.toLowerCase().contains("account") || tableName.toLowerCase().contains("card") || tableName.toLowerCase().contains("bank")) {
            list.add(new MockField("card_number", "6222021234567890123", "银行卡号", "L4 绝密高危"));
            list.add(new MockField("cvv_code", "888", "CVV安全校验码", "L5 极高危鉴权"));
            list.add(new MockField("account_balance", "9982443.53", "账户余额", "L3 敏感机密"));
        } else {
            list.add(new MockField("contact_phone", "13911112222", "联系电话", "L3 敏感机密"));
            list.add(new MockField("id_number", "310101198501011234", "证件号码", "L4 绝密高危"));
            list.add(new MockField("ip_address", "192.168.1.100", "IP地址", "L1 对外公开"));
        }
        return list;
    }

    private static class MockField {
        String name;
        String sampleValue;
        String categoryName;
        String gradeName;

        MockField(String name, String sampleValue, String categoryName, String gradeName) {
            this.name = name;
            this.sampleValue = sampleValue;
            this.categoryName = categoryName;
            this.gradeName = gradeName;
        }
    }
}
