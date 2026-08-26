package com.yss.datamiddle.smartgovernance.domain.llm;

/**
 * 统一大模型通信网关接口 (防腐层)
 */
public interface LlmGateway {
    /**
     * 执行字段安全分类分级上下文推理
     *
     * @param payload 纯元数据白名单 Payload
     * @return 结构化判定结论
     */
    LlmInferenceResult inferSecurityClassification(PromptPayload payload);

    /**
     * 计算两段指标口径的语义相似度 (0.0 ~ 1.0)
     */
    Double calculateMetricSemanticSimilarity(String metricNameA, String definitionA, String metricNameB, String definitionB);
}
