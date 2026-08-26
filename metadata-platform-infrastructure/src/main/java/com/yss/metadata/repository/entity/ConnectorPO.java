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
 * 数据源（连接器）持久化对象。
 *
 * <p>对应 data_source 表（数据架构 §5）；id 采用 VARCHAR(36) UUID（冻结 OpenAPI id 为 string，
 * 由应用层生成，@TableId 使用 IdType.INPUT 手动赋值）；
 * 凭据仅存加密引用 cred_ref，密码明文不落库。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("data_source")
public class ConnectorPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("host")
    private String host;

    @TableField("port")
    private Integer port;

    @TableField("dialect")
    private String dialect;

    @TableField("username")
    private String username;

    @TableField("cred_ref")
    private String credentialRef;

    @TableField("auto_classify")
    private Boolean autoClassify;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
