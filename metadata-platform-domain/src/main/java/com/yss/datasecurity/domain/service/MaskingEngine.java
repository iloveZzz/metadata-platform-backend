package com.yss.datasecurity.domain.service;

import com.yss.datasecurity.domain.constant.MaskingConstants;
import com.yss.datasecurity.domain.enums.CryptoAlgorithmEnum;
import com.yss.datasecurity.domain.enums.MaskingAlgorithmTypeEnum;
import com.yss.datasecurity.domain.model.MaskingRule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Component
public class MaskingEngine {

    public Object maskValue(Object rawValue, MaskingRule rule) {
        if (rawValue == null) {
            return null;
        }
        String strVal = String.valueOf(rawValue);
        if (strVal.isEmpty()) {
            return strVal;
        }

        String algoType = rule != null ? rule.getAlgorithmType() : MaskingAlgorithmTypeEnum.MASKING.getCode();
        Map<String, Object> params = rule != null && rule.getAlgorithmParams() != null 
            ? rule.getAlgorithmParams() 
            : new HashMap<>();

        MaskingAlgorithmTypeEnum typeEnum = MaskingAlgorithmTypeEnum.of(algoType);
        switch (typeEnum) {
            case MASKING:
                return applyMasking(strVal, params);
            case HASH_SALT:
                return applyHashSalt(strVal, params);
            case ENCRYPTION:
                return applyEncryption(strVal, params);
            case SPECIAL:
                return applySpecial(strVal, params);
            default:
                return applyDefaultFallback(strVal);
        }
    }

    public Object applyDefaultFallback(Object rawValue) {
        if (rawValue == null) return null;
        String str = String.valueOf(rawValue);
        int len = str.length();
        if (len <= 2) {
            return "**";
        }
        int start = len / 3;
        int end = len - (len / 3);
        StringBuilder sb = new StringBuilder();
        sb.append(str, 0, start);
        for (int i = start; i < end; i++) {
            sb.append('*');
        }
        sb.append(str.substring(end));
        return sb.toString();
    }

    private String applyMasking(String val, Map<String, Object> params) {
        int len = val.length();
        int start = getIntParam(params, MaskingConstants.PARAM_START, MaskingConstants.DEFAULT_MASK_START);
        int end = getIntParam(params, MaskingConstants.PARAM_END, MaskingConstants.DEFAULT_MASK_END);
        String maskChar = getStringParam(params, MaskingConstants.PARAM_MASK_CHAR, MaskingConstants.DEFAULT_MASK_CHAR);

        if (start < 0) start = 0;
        if (start > len) start = len;
        if (end < start) end = start;
        if (end > len) end = len;

        StringBuilder sb = new StringBuilder();
        sb.append(val, 0, start);
        for (int i = start; i < end; i++) {
            sb.append(maskChar);
        }
        sb.append(val.substring(end));
        return sb.toString();
    }

    private String applyHashSalt(String val, Map<String, Object> params) {
        String hashType = getStringParam(params, MaskingConstants.PARAM_HASH_TYPE, MaskingConstants.DEFAULT_HASH_TYPE);
        String salt = getStringParam(params, MaskingConstants.PARAM_SALT, MaskingConstants.DEFAULT_SALT);
        try {
            MessageDigest md = MessageDigest.getInstance(CryptoAlgorithmEnum.MD5.getCode().equalsIgnoreCase(hashType) ? "MD5" : "SHA-256");
            byte[] digest = md.digest((val + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "HASH(" + val.hashCode() + ")";
        }
    }

    private String applyEncryption(String val, Map<String, Object> params) {
        String algo = getStringParam(params, MaskingConstants.PARAM_ALGORITHM, MaskingConstants.ALGO_FPE_FF1);
        if (MaskingConstants.ALGO_FPE_FF1.equalsIgnoreCase(algo)) {
            // 格式保留加密模拟：数字映射保持长度和数字类型
            StringBuilder sb = new StringBuilder();
            for (char c : val.toCharArray()) {
                if (Character.isDigit(c)) {
                    int d = (Character.getNumericValue(c) + 7) % 10;
                    sb.append(d);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        return "ENC(" + algo + ":" + val.hashCode() + ")";
    }

    private Object applySpecial(String val, Map<String, Object> params) {
        String replaceType = getStringParam(params, "replaceType", "FIXED_STRING");
        if ("NULL".equalsIgnoreCase(replaceType)) {
            return null;
        }
        return getStringParam(params, "fixedString", "***");
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object val = params.get(key);
        return val != null ? String.valueOf(val) : defaultValue;
    }
}
