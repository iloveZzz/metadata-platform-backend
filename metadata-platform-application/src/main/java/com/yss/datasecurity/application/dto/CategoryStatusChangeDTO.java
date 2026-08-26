package com.yss.datasecurity.application.dto;

import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatusChangeDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "状态不能为空")
    private String status; // ENABLED / DISABLED

    @Builder.Default
    private String disablePolicy = "RETAIN_TAGS"; // RETAIN_TAGS / DELETE_TAGS
}
