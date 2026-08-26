package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import com.yss.metadata.domain.connector.model.ConnectorType;
import com.yss.metadata.domain.connector.model.Dialect;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 新增连接器命令（冻结 OpenAPI ConnectorCreate）。
 */
@Getter
@Setter
public class ConnectorAddCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 连接器名称（唯一） */
    @NotBlank(message = "连接器名称不能为空")
    private String name;

    /** 连接器类型 */
    @NotNull(message = "连接器类型不能为空")
    private ConnectorType type;

    /** 主机地址 */
    @NotBlank(message = "主机地址不能为空")
    private String host;

    /** 端口 */
    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口必须在 1-65535 之间")
    @Max(value = 65535, message = "端口必须在 1-65535 之间")
    private Integer port;

    /** 方言（可选，缺省默认 AUTO；冻结 OpenAPI ConnectorCreate.dialect 非必填） */
    private Dialect dialect;

    /** 用户名 */
    private String username;

    /** 密码（加密存储，不落库明文） */
    private String password;

    /** 是否自动识别分类（默认 true） */
    private Boolean autoClassify = Boolean.TRUE;
}
