package com.yss.datasecurity.domain.constant;

/**
 * 数据脱敏引擎常量类
 */
public final class MaskingConstants {

    private MaskingConstants() {}

    /** 掩码起始索引参数名 */
    public static final String PARAM_START = "start";

    /** 掩码结束索引参数名 */
    public static final String PARAM_END = "end";

    /** 掩码替换字符参数名 */
    public static final String PARAM_MASK_CHAR = "maskChar";

    /** 哈希类型参数名 */
    public static final String PARAM_HASH_TYPE = "hashType";

    /** 加盐参数名 */
    public static final String PARAM_SALT = "salt";

    /** 算法参数名 */
    public static final String PARAM_ALGORITHM = "algorithm";

    /** 默认掩码字符 */
    public static final String DEFAULT_MASK_CHAR = "*";

    /** 默认系统加盐 */
    public static final String DEFAULT_SALT = "sec_salt_yss";

    /** 默认哈希算法 */
    public static final String DEFAULT_HASH_TYPE = "SHA-256";

    /** FPE 格式保留加密算法标识 */
    public static final String ALGO_FPE_FF1 = "FPE_FF1";

    /** 默认掩码起始位 */
    public static final int DEFAULT_MASK_START = 3;

    /** 默认掩码结束位 */
    public static final int DEFAULT_MASK_END = 7;
}
