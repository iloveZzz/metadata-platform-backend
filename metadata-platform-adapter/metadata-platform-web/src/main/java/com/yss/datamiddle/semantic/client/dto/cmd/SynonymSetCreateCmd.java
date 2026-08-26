package com.yss.datamiddle.semantic.client.dto.cmd;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SynonymSetCreateCmd {
    @NotBlank(message = "同义词组名称不能为空")
    private String name;

    @NotBlank(message = "主词不能为空")
    private String canonical;

    @NotEmpty(message = "同义词列表不能为空")
    private List<String> words;

    private Long termId;
}
