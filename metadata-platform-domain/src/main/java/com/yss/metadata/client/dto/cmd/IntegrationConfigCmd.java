package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * 集成配置命令（PUT /api/integrations；冻结 API 未声明 requestBody，
 * 以本 Cmd 为契约：Gravitino 端点/认证/启用 + DataHub 端点/认证 + test 标记）。
 *
 * <p>test=true 时先测试 Gravitino 连接：成功则保存（含 lastTest），失败抛
 * ConnectTestException（422 分类）不保存；test=false/缺省仅保存。
 * 认证令牌明文入参，持久化经凭据加密引用（seam，同 data_source.cred_ref）。</p>
 */
@Getter
@Setter
public class IntegrationConfigCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** Gravitino 端点地址 */
    private String gravitinoEndpoint;

    /** Gravitino 认证令牌（明文入参，持久化走加密引用） */
    private String gravitinoAuthToken;

    /** Gravitino 上游是否启用（缺省 false） */
    private Boolean gravitinoEnabled;

    /** DataHub 导出目标地址 */
    private String datahubEndpoint;

    /** DataHub 认证令牌（明文入参，持久化走加密引用） */
    private String datahubAuthToken;

    /** 是否同时测试 Gravitino 连接（true=测试并保存；false/缺省=仅保存） */
    private Boolean test;
}
