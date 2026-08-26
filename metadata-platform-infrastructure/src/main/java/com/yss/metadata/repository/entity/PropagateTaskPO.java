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
 * 分类传播异步任务持久化对象（propagate_task 表；WU-04-04）。
 *
 * <p>幂等：同 classification_id+version 任务（任意状态）复用（服务层）；
 * 状态流转 pending→running→success/failed；coverage 覆盖范围可核验。</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("propagate_task")
public class PropagateTaskPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("classification_id")
    private String classificationId;

    @TableField("version")
    private String version;

    @TableField("status")
    private String status;

    @TableField("coverage")
    private String coverage;

    @TableField("operator")
    private String operator;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
