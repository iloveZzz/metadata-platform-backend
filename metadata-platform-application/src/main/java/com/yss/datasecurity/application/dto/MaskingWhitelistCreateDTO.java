package com.yss.datasecurity.application.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yss.cloud.dto.CommandDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingWhitelistCreateDTO extends CommandDTO {
    private static final long serialVersionUID = 1L;
    @JsonAlias({"subjectType", "granteeType"})
    @NotBlank(message = "授权主体类型不能为空 (USER, ROLE, APP)")
    private String granteeType;

    @JsonAlias({"subjectId", "granteeId"})
    @NotBlank(message = "授权主体标识 (用户ID/角色编码) 不能为空")
    private String granteeId;

    @JsonAlias({"categoryId", "category_id"})
    private Long categoryId;

    @JsonAlias({"ruleId", "maskingRuleId", "rule_id"})
    private Long ruleId;

    @JsonAlias({"startTime", "validStartTime", "start_time"})
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "生效起始时间不能为空")
    private LocalDateTime startTime;

    @JsonAlias({"endTime", "validEndTime", "end_time"})
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "生效截止时间不能为空")
    private LocalDateTime endTime;

    @JsonAlias({"whitelistName", "whitelist_name"})
    private String whitelistName;

    private String reason;
}
