package com.yss.datasecurity.domain.constant;

/**
 * 数据安全中心公共业务常量类
 */
public final class DataSecurityConstants {

    private DataSecurityConstants() {}

    /** 默认管理员操作人 */
    public static final String DEFAULT_OPERATOR = "admin";

    /** 默认数据安全管理员 */
    public static final String DEFAULT_SECURITY_ADMIN = "安全管理员";

    /** 默认数据源标识 */
    public static final String DEFAULT_DATASOURCE_ID = "default_ds";

    /** 默认数据源名称 */
    public static final String DEFAULT_DATASOURCE_NAME = "default_datasource";

    /** 默认 Schema 库名 */
    public static final String DEFAULT_SCHEMA_NAME = "default_schema";

    /** 资产来源：DATAPHIN */
    public static final String ASSET_SOURCE_DATAPHIN = "DATAPHIN";

    /** 资产来源：DATASOURCE */
    public static final String ASSET_SOURCE_DATASOURCE = "DATASOURCE";

    /** 默认通用主键生成范围基数 */
    public static final int DEFAULT_ID_RANDOM_BOUND = 10000;
}
