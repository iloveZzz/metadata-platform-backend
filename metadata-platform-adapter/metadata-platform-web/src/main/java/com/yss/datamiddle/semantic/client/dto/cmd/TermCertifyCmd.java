package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 认证 / 弃用命令（冻结契约 CertifyRequest；action=certify|deprecate，幂等 + 审计）。
 */
@Getter
@Setter
public class TermCertifyCmd extends CommandDTO {

    @NotBlank(message = "认证动作不能为空（certify / deprecate）")
    private String action;

    /** 操作备注（写入审计） */
    private String note;
}
