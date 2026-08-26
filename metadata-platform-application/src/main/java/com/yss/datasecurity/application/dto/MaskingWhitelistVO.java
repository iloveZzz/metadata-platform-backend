package com.yss.datasecurity.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskingWhitelistVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String granteeType;
    @JsonProperty("subjectType")
    public String getSubjectType() {
        return granteeType;
    }

    private String granteeId;
    @JsonProperty("subjectId")
    public String getSubjectId() {
        return granteeId;
    }

    private String whitelistName;
    @JsonProperty("whitelistName")
    public String getWhitelistName() {
        return whitelistName != null ? whitelistName : ((granteeId != null ? granteeId : "") + " 时效免脱敏白名单");
    }

    private Long categoryId;
    private String categoryName;
    private Long ruleId;
    private String ruleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonProperty("validStartTime")
    public LocalDateTime getValidStartTime() {
        return startTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @JsonProperty("validEndTime")
    public LocalDateTime getValidEndTime() {
        return endTime;
    }

    private String status;
    private String reason;
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
