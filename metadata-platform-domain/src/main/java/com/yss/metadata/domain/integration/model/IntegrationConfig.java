package com.yss.metadata.domain.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成配置（数据架构 IntegrationConfig：单例行 id=1；Gravitino 上游 + DataHub 导出目标）。
 *
 * <p>单例配置行（id=1，upsert）；凭据加密引用（seam，同 data_source.cred_ref 约定）；
 * Gravitino 测试连接失败分类（network/credential/dialect）；保存写 updated_at。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 单例配置行主键（固定 "1"） */
    public static final String SINGLETON_ID = "1";

    /** 单例配置行主键（固定 "1"） */
    private String id;

    /** Gravitino 端点地址 */
    private String gravitinoEndpoint;

    /** Gravitino 认证（加密引用） */
    private String gravitinoAuthRef;

    /** Gravitino 上游是否启用 */
    private Boolean gravitinoEnabled;

    /** 最近测试连接结果（时间/状态/分类） */
    private String gravitinoLastTest;

    /** DataHub 导出目标地址 */
    private String datahubEndpoint;

    /** DataHub 认证（加密引用） */
    private String datahubAuthRef;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
