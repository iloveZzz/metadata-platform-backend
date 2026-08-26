package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新术语命令（冻结契约 TermUpdate；乐观锁 version 必须携带，过期 409 VERSION_CONFLICT）。
 */
@Getter
@Setter
public class TermUpdateCmd extends CommandDTO {

    @NotBlank(message = "术语名称不能为空")
    private String name;

    private List<String> aliases;

    @NotBlank(message = "术语定义不能为空")
    private String definition;

    private String description;

    @NotBlank(message = "负责人不能为空")
    private String owner;

    @NotNull(message = "乐观锁版本号必须携带")
    private Integer version;
}
