package com.yss.datamiddle.semantic.client.dto.cmd;

import com.yss.cloud.dto.CommandDTO;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SearchExpandCmd extends CommandDTO {
    @NotEmpty(message = "查询词列表不能为空")
    private List<String> queries;
}
