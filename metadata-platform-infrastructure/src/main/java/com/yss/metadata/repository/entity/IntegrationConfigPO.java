package com.yss.metadata.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成配置持久化对象（integration_config 表；单例行 id=1；WU-05-01）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("integration_config")
public class IntegrationConfigPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("gravitino_endpoint")
    private String gravitinoEndpoint;

    @TableField("gravitino_auth_ref")
    private String gravitinoAuthRef;

    @TableField("gravitino_enabled")
    private Boolean gravitinoEnabled;

    @TableField("gravitino_last_test")
    private String gravitinoLastTest;

    @TableField("datahub_endpoint")
    private String datahubEndpoint;

    @TableField("datahub_auth_ref")
    private String datahubAuthRef;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
