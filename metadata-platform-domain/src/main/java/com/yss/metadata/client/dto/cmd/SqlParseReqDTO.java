package com.yss.metadata.client.dto.cmd;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * SQL 实时血缘解析请求 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("SQL 实时血缘解析请求")
public class SqlParseReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "sql 不能为空")
    @ApiModelProperty(value = "待解析的 SQL 语句", required = true, example = "CREATE VIEW dwd_orders AS SELECT order_id, sum(amount) AS total_amt FROM ods_orders GROUP BY order_id")
    private String sql;

    @ApiModelProperty(value = "SQL 方言提示（可选，例如 mysql, oceanbase, gaussdb）", example = "mysql")
    private String dialect;
}
