package com.yss.smartdiscovery.infrastructure.llm;

import java.util.List;

public class StrictMetadataPromptBuilder {

    public static String buildTaggingPrompt(String tableName, String columnName, String columnComment, String dataType, String fewShotTemplate) {
        // 安全红线断言：确保无真实行数据参数
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深金融数据架构师。请仅依据以下数据表结构元数据与中文注释，推导该字段的业务主题或安全等级标签。\n");
        prompt.append("【严格要求】禁止猜测真实数据值，仅分析元数据定义。\n\n");
        prompt.append("【字段元数据】\n");
        prompt.append("表名: ").append(tableName).append("\n");
        prompt.append("字段名: ").append(columnName).append("\n");
        prompt.append("数据类型: ").append(dataType).append("\n");
        prompt.append("字段注释: ").append(columnComment).append("\n\n");

        if (fewShotTemplate != null && !fewShotTemplate.trim().isEmpty()) {
            prompt.append("【参考规则与示例】\n").append(fewShotTemplate).append("\n\n");
        }

        prompt.append("请输出 JSON 格式：{\"recommendedTag\": \"...\", \"confidence\": 0.85, \"reason\": \"...\"}");
        return prompt.toString();
    }

    public static String buildAskMetadataPrompt(String userQuery, List<String> availableSchemas) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位企业级数据资产找数专家。用户输入了找数需求，请提取意图实体、业务口径并匹配最合适的数据资产。\n\n");
        prompt.append("【用户查询】: ").append(userQuery).append("\n\n");
        prompt.append("【可用资产元数据目录】:\n");
        for (String schema : availableSchemas) {
            prompt.append("- ").append(schema).append("\n");
        }
        prompt.append("\n请输出意图分析实体、关联认证口径及 Top 推荐资产。");
        return prompt.toString();
    }
}
