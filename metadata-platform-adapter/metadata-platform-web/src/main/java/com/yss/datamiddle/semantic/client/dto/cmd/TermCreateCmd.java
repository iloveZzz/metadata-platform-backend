package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 新建术语命令（冻结契约 TermCreate；保存为草稿；owner 创建必填 SB-01）。
 */
@Getter
@Setter
public class TermCreateCmd extends CommandDTO {

    @NotBlank(message = "术语名称不能为空")
    private String name;

    private List<String> aliases;

    @NotBlank(message = "术语定义不能为空")
    private String definition;

    private String description;

    @NotBlank(message = "负责人不能为空（SB-01 创建必填）")
    private String owner;
}
