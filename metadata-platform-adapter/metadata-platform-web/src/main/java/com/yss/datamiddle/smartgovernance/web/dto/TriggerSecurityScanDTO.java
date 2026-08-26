package com.yss.datamiddle.smartgovernance.web.dto;

import java.io.Serializable;

import lombok.Data;

@Data
public class TriggerSecurityScanDTO implements Serializable {
    private String templateId;
    private String dataSource;
    private String databaseName;
    private String tableName;
}
