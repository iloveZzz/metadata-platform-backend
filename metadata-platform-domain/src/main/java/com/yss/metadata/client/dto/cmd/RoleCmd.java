package com.yss.metadata.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色创建命令（POST /api/roles；冻结 API 未声明 requestBody，
 * 以本 Cmd 为契约：name/scope/domains）。
 *
 * <p>name 必填且唯一（重复 409 role.name_conflict）；scope 为范围描述文本；
 * domains 为数据域绑定（data_domain 幂等 upsert by name + role_domain 绑定，
 * 角色删除前 refs=绑定数，refs>0 删除 409）。</p>
 */
@Getter
@Setter
public class RoleCmd extends CommandDTO {

    private static final long serialVersionUID = 1L;

    /** 角色名（唯一，必填） */
    private String name;

    /** 角色范围描述（如"交易/客户/财务域"） */
    private String scope;

    /** 数据域绑定（按名称；幂等 upsert） */
    private List<String> domains = new ArrayList<>();
}
