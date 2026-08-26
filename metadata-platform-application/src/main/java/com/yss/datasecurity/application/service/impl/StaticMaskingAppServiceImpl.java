package com.yss.datasecurity.application.service.impl;

import com.yss.datasecurity.application.dto.InstallPackageDTO;
import com.yss.datasecurity.application.dto.ProjectPackageVO;
import com.yss.datasecurity.application.dto.StaticAlgorithmVO;
import com.yss.datasecurity.application.dto.StaticMaskTestDTO;
import com.yss.datasecurity.application.dto.StaticMaskTestResultVO;
import com.yss.datasecurity.application.service.StaticMaskingAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticMaskingAppServiceImpl implements StaticMaskingAppService {

    private final List<StaticAlgorithmVO> algorithmLibrary = new ArrayList<>();
    private final Map<String, ProjectPackageVO> projectPackageStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initAlgorithms();
        initProjectPackages();
    }

    private void initAlgorithms() {
        algorithmLibrary.clear();
        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(1L)
                .functionName("sec_mask_phone")
                .displayName("手机号码遮盖掩码")
                .algorithmType("MASK")
                .description("保留手机号前3位与后4位，中间4位替换为掩码字符（默认*）")
                .signature("sec_mask_phone(column_name, [mask_char])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL", "Flink"))
                .sampleInput("13812345678")
                .sampleOutput("138****5678")
                .sqlExample("SELECT user_id, sec_mask_phone(mobile) AS mobile FROM prod_user_info")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(2L)
                .functionName("sec_mask_idcard")
                .displayName("居民身份证号掩码")
                .algorithmType("MASK")
                .description("保留身份证前6位与后4位，中间8位（生日部分）替换为掩码字符")
                .signature("sec_mask_idcard(column_name, [mask_char])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL", "Flink"))
                .sampleInput("110101199003072345")
                .sampleOutput("110101********2345")
                .sqlExample("SELECT id, name, sec_mask_idcard(id_card) AS id_card FROM prod_cust_identity")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(3L)
                .functionName("sec_mask_name")
                .displayName("中文姓名遮盖掩码")
                .algorithmType("MASK")
                .description("2字姓名遮盖第2字（如'张*'），3字及以上保留首尾字、中间全部遮盖（如'李*明'）")
                .signature("sec_mask_name(column_name)")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL"))
                .sampleInput("张三封")
                .sampleOutput("张*封")
                .sqlExample("SELECT sec_mask_name(cust_name) AS cust_name FROM prod_account")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(4L)
                .functionName("sec_mask_bankcard")
                .displayName("银行卡号遮盖掩码")
                .algorithmType("MASK")
                .description("保留银行卡前6位与后4位，中间卡号全部遮盖为星号")
                .signature("sec_mask_bankcard(column_name)")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL", "Flink"))
                .sampleInput("6222021001122334455")
                .sampleOutput("622202*********4455")
                .sqlExample("SELECT card_id, sec_mask_bankcard(card_no) AS card_no FROM prod_card_binding")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(5L)
                .functionName("sec_mask_email")
                .displayName("电子邮箱遮盖掩码")
                .algorithmType("MASK")
                .description("保留邮箱用户名首字符与@域名，其余用户名字符遮盖为***")
                .signature("sec_mask_email(column_name)")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL"))
                .sampleInput("zhangsan@yss.com.cn")
                .sampleOutput("z***@yss.com.cn")
                .sqlExample("SELECT user_id, sec_mask_email(email) AS email FROM prod_user_contact")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(6L)
                .functionName("sec_hash_sha256")
                .displayName("加盐SHA256哈希脱敏")
                .algorithmType("HASH")
                .description("使用不可逆安全哈希函数及动态Salt，生成64位十六进制不可逆特征摘要")
                .signature("sec_hash_sha256(column_name, [salt_key])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL", "Flink"))
                .sampleInput("SecretPassword123")
                .sampleOutput("8f14e45fceea167a5a36dedd4bea2543...")
                .sqlExample("SELECT id, sec_hash_sha256(token, 'sec_salt_yss') AS token_hash FROM prod_token_store")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(7L)
                .functionName("sec_hash_md5")
                .displayName("加盐MD5哈希脱敏")
                .algorithmType("HASH")
                .description("加盐MD5哈希算法，生成32位特征散列串")
                .signature("sec_hash_md5(column_name, [salt_key])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL"))
                .sampleInput("SampleRawKey")
                .sampleOutput("e10adc3949ba59abbe56e057f20f883e")
                .sqlExample("SELECT sec_hash_md5(device_id) AS device_hash FROM prod_device_log")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(8L)
                .functionName("sec_crypto_fpe")
                .displayName("FPE格式保留原生加密")
                .algorithmType("CRYPTO")
                .description("保留格式加密(FF1算法)，密文与明文长度、字符集完全一致，支持主键与关联外键安全计算")
                .signature("sec_crypto_fpe(column_name, [key_ref], [tweak])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute"))
                .sampleInput("13812345678")
                .sampleOutput("18589012345")
                .sqlExample("SELECT sec_crypto_fpe(account_no, 'key_fpe_prod_01') AS enc_account FROM prod_trade_order")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(9L)
                .functionName("sec_mask_custom")
                .displayName("自定义区间掩码")
                .algorithmType("MASK")
                .description("指定脱敏起止位(start, end)与替换字符，灵活自定义遮盖区间")
                .signature("sec_mask_custom(column_name, start_idx, end_idx, [mask_char])")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL"))
                .sampleInput("ABCDEFGHIJKL")
                .sampleOutput("ABC****IJKL")
                .sqlExample("SELECT sec_mask_custom(serial_no, 3, 7, '*') AS serial_no FROM prod_serial_table")
                .build());

        algorithmLibrary.add(StaticAlgorithmVO.builder()
                .id(10L)
                .functionName("sec_mask_null")
                .displayName("敏感字段置空 (NULL)")
                .algorithmType("OTHER")
                .description("直接将敏感数据列返回 NULL 空值，彻底阻断敏感信息暴露")
                .signature("sec_mask_null(column_name)")
                .supportedEngines(Arrays.asList("Spark SQL", "Hive", "MaxCompute", "MySQL", "Flink"))
                .sampleInput("AnyConfidentialData")
                .sampleOutput("NULL")
                .sqlExample("SELECT user_id, sec_mask_null(credit_score) AS credit_score FROM prod_credit_info")
                .build());
    }

    private void initProjectPackages() {
        projectPackageStore.clear();

        ProjectPackageVO p1 = ProjectPackageVO.builder()
                .id(101L)
                .projectId("prj_default")
                .projectName("默认数据开发项目")
                .projectCode("PRJ_DEFAULT")
                .engineType("Spark SQL / Hive")
                .packageVersion("v1.5.0-standard")
                .status("INSTALLED")
                .authorizedCount(10)
                .authorizedFunctions(Arrays.asList("sec_mask_phone", "sec_mask_idcard", "sec_mask_name", "sec_mask_bankcard", "sec_mask_email", "sec_hash_sha256", "sec_hash_md5", "sec_crypto_fpe", "sec_mask_custom", "sec_mask_null"))
                .installedAt("2026-08-15 10:20:00")
                .installedBy("安全管理员")
                .build();

        ProjectPackageVO p2 = ProjectPackageVO.builder()
                .id(102L)
                .projectId("prj_investment")
                .projectName("投研数据分析项目")
                .projectCode("PRJ_INVESTMENT")
                .engineType("Spark SQL")
                .packageVersion("v1.5.0-standard")
                .status("INSTALLED")
                .authorizedCount(10)
                .authorizedFunctions(Arrays.asList("sec_mask_phone", "sec_mask_idcard", "sec_mask_name", "sec_mask_bankcard", "sec_mask_email", "sec_hash_sha256", "sec_hash_md5", "sec_crypto_fpe", "sec_mask_custom", "sec_mask_null"))
                .installedAt("2026-08-18 14:35:12")
                .installedBy("李安全")
                .build();

        ProjectPackageVO p3 = ProjectPackageVO.builder()
                .id(103L)
                .projectId("prj_risk")
                .projectName("风控集市计算项目")
                .projectCode("PRJ_RISK")
                .engineType("MaxCompute")
                .packageVersion("v1.4.2-standard")
                .status("UPGRADABLE")
                .authorizedCount(8)
                .authorizedFunctions(Arrays.asList("sec_mask_phone", "sec_mask_idcard", "sec_mask_name", "sec_mask_bankcard", "sec_hash_sha256", "sec_hash_md5", "sec_mask_custom", "sec_mask_null"))
                .installedAt("2026-07-10 09:00:00")
                .installedBy("系统管理员")
                .build();

        ProjectPackageVO p4 = ProjectPackageVO.builder()
                .id(104L)
                .projectId("prj_regulatory")
                .projectName("监管报送数据项目")
                .projectCode("PRJ_REGULATORY")
                .engineType("Hive / Spark SQL")
                .packageVersion("v1.5.0-standard")
                .status("INSTALLED")
                .authorizedCount(10)
                .authorizedFunctions(Arrays.asList("sec_mask_phone", "sec_mask_idcard", "sec_mask_name", "sec_mask_bankcard", "sec_mask_email", "sec_hash_sha256", "sec_hash_md5", "sec_crypto_fpe", "sec_mask_custom", "sec_mask_null"))
                .installedAt("2026-08-20 16:48:30")
                .installedBy("安全审计员")
                .build();

        ProjectPackageVO p5 = ProjectPackageVO.builder()
                .id(105L)
                .projectId("prj_marketing")
                .projectName("智能营销应用项目")
                .projectCode("PRJ_MARKETING")
                .engineType("MySQL / Flink")
                .packageVersion("-")
                .status("NOT_INSTALLED")
                .authorizedCount(0)
                .authorizedFunctions(Collections.emptyList())
                .installedAt("-")
                .installedBy("-")
                .build();

        projectPackageStore.put(p1.getProjectId(), p1);
        projectPackageStore.put(p2.getProjectId(), p2);
        projectPackageStore.put(p3.getProjectId(), p3);
        projectPackageStore.put(p4.getProjectId(), p4);
        projectPackageStore.put(p5.getProjectId(), p5);
    }

    @Override
    public List<StaticAlgorithmVO> listAlgorithms(String keyword, String algorithmType) {
        return algorithmLibrary.stream()
                .filter(algo -> {
                    if (algorithmType != null && !algorithmType.trim().isEmpty() && !algorithmType.equalsIgnoreCase("ALL")) {
                        if (!algorithmType.equalsIgnoreCase(algo.getAlgorithmType())) {
                            return false;
                        }
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchName = algo.getFunctionName() != null && algo.getFunctionName().toLowerCase().contains(kw);
                        boolean matchDisplay = algo.getDisplayName() != null && algo.getDisplayName().toLowerCase().contains(kw);
                        boolean matchDesc = algo.getDescription() != null && algo.getDescription().toLowerCase().contains(kw);
                        return matchName || matchDisplay || matchDesc;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectPackageVO> listProjectPackages(String keyword, String status) {
        return projectPackageStore.values().stream()
                .filter(pkg -> {
                    if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                        if (!status.equalsIgnoreCase(pkg.getStatus())) {
                            return false;
                        }
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchName = pkg.getProjectName() != null && pkg.getProjectName().toLowerCase().contains(kw);
                        boolean matchCode = pkg.getProjectCode() != null && pkg.getProjectCode().toLowerCase().contains(kw);
                        return matchName || matchCode;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean installPackage(InstallPackageDTO dto) {
        ProjectPackageVO existing = projectPackageStore.get(dto.getProjectId());
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<String> functions = dto.getAuthorizedFunctions() != null && !dto.getAuthorizedFunctions().isEmpty()
                ? dto.getAuthorizedFunctions()
                : algorithmLibrary.stream().map(StaticAlgorithmVO::getFunctionName).collect(Collectors.toList());

        ProjectPackageVO updated = ProjectPackageVO.builder()
                .id(existing != null ? existing.getId() : System.currentTimeMillis())
                .projectId(dto.getProjectId())
                .projectName(dto.getProjectName() != null ? dto.getProjectName() : (existing != null ? existing.getProjectName() : dto.getProjectId()))
                .projectCode(existing != null ? existing.getProjectCode() : dto.getProjectId().toUpperCase())
                .engineType(dto.getEngineType() != null ? dto.getEngineType() : (existing != null ? existing.getEngineType() : "Spark SQL / Hive"))
                .packageVersion(dto.getPackageVersion())
                .status("INSTALLED")
                .authorizedCount(functions.size())
                .authorizedFunctions(functions)
                .installedAt(nowStr)
                .installedBy("安全管理员")
                .build();

        projectPackageStore.put(dto.getProjectId(), updated);
        log.info("成功为项目 [{}] 安装安全算法包 [{}]", dto.getProjectId(), dto.getPackageVersion());
        return true;
    }

    @Override
    public StaticMaskTestResultVO testAlgorithm(StaticMaskTestDTO dto) {
        long start = System.currentTimeMillis();
        String func = dto.getFunctionName() != null ? dto.getFunctionName().toLowerCase() : "sec_mask_phone";
        String raw = dto.getRawValue() != null ? dto.getRawValue() : "";
        Map<String, Object> params = dto.getParams() != null ? dto.getParams() : Collections.emptyMap();

        String masked;
        String algoType;
        String sqlSnippet;

        switch (func) {
            case "sec_mask_phone":
                algoType = "MASK";
                masked = maskPhone(raw);
                sqlSnippet = "SELECT sec_mask_phone(mobile) FROM target_table";
                break;
            case "sec_mask_idcard":
                algoType = "MASK";
                masked = maskIdCard(raw);
                sqlSnippet = "SELECT sec_mask_idcard(id_card) FROM target_table";
                break;
            case "sec_mask_name":
                algoType = "MASK";
                masked = maskChineseName(raw);
                sqlSnippet = "SELECT sec_mask_name(cust_name) FROM target_table";
                break;
            case "sec_mask_bankcard":
                algoType = "MASK";
                masked = maskBankCard(raw);
                sqlSnippet = "SELECT sec_mask_bankcard(card_no) FROM target_table";
                break;
            case "sec_mask_email":
                algoType = "MASK";
                masked = maskEmail(raw);
                sqlSnippet = "SELECT sec_mask_email(email) FROM target_table";
                break;
            case "sec_hash_sha256":
                algoType = "HASH";
                String salt = String.valueOf(params.getOrDefault("salt", "sec_salt_yss"));
                masked = hashSha256(raw, salt);
                sqlSnippet = "SELECT sec_hash_sha256(token, '" + salt + "') FROM target_table";
                break;
            case "sec_hash_md5":
                algoType = "HASH";
                String md5Salt = String.valueOf(params.getOrDefault("salt", "sec_salt_yss"));
                masked = hashMd5(raw, md5Salt);
                sqlSnippet = "SELECT sec_hash_md5(device_id, '" + md5Salt + "') FROM target_table";
                break;
            case "sec_crypto_fpe":
                algoType = "CRYPTO";
                masked = encryptFpe(raw);
                sqlSnippet = "SELECT sec_crypto_fpe(account_no, 'key_fpe_prod') FROM target_table";
                break;
            case "sec_mask_custom":
                algoType = "MASK";
                int sIdx = getInt(params, "start", 3);
                int eIdx = getInt(params, "end", 7);
                String maskChar = String.valueOf(params.getOrDefault("maskChar", "*"));
                masked = maskCustom(raw, sIdx, eIdx, maskChar);
                sqlSnippet = String.format("SELECT sec_mask_custom(col, %d, %d, '%s') FROM target_table", sIdx, eIdx, maskChar);
                break;
            case "sec_mask_null":
                algoType = "OTHER";
                masked = "NULL";
                sqlSnippet = "SELECT sec_mask_null(credit_score) FROM target_table";
                break;
            default:
                algoType = "MASK";
                masked = maskPhone(raw);
                sqlSnippet = "SELECT " + func + "(col) FROM target_table";
                break;
        }

        long costMs = Math.max(1, System.currentTimeMillis() - start);

        return StaticMaskTestResultVO.builder()
                .functionName(func)
                .rawValue(raw)
                .maskedValue(masked)
                .costMs(costMs)
                .algorithmType(algoType)
                .sqlSnippet(sqlSnippet)
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        int mid = phone.length() / 2;
        return phone.substring(0, Math.max(0, mid - 2)) + "****" + phone.substring(Math.min(phone.length(), mid + 2));
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        if (idCard.length() == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }
        return idCard.substring(0, 4) + "******" + idCard.substring(idCard.length() - 4);
    }

    private String maskChineseName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 2) {
            return name.substring(0, 1) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(name.charAt(0));
        for (int i = 1; i < name.length() - 1; i++) {
            sb.append('*');
        }
        sb.append(name.charAt(name.length() - 1));
        return sb.toString();
    }

    private String maskBankCard(String card) {
        if (card == null || card.length() < 10) return card;
        int len = card.length();
        StringBuilder sb = new StringBuilder();
        sb.append(card, 0, 6);
        for (int i = 6; i < len - 4; i++) {
            sb.append('*');
        }
        sb.append(card.substring(len - 4));
        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf('@');
        String name = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (name.length() <= 1) {
            return name + "***" + domain;
        }
        return name.charAt(0) + "***" + domain;
    }

    private String hashSha256(String raw, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((raw + (salt != null ? salt : "")).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            log.error("SHA-256 哈希脱敏失败, raw={}", raw, e);
            throw new IllegalStateException("SHA-256 脱敏计算失败", e);
        }
    }

    private String hashMd5(String raw, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest((raw + (salt != null ? salt : "")).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            log.error("MD5 哈希脱敏失败, raw={}", raw, e);
            throw new IllegalStateException("MD5 脱敏计算失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String encryptFpe(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) {
                int d = (Character.getNumericValue(c) + 7) % 10;
                sb.append(d);
            } else if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int offset = (c - base + 5) % 26;
                sb.append((char) (base + offset));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String maskCustom(String raw, int start, int end, String maskChar) {
        if (raw == null) return null;
        int len = raw.length();
        if (start < 0) start = 0;
        if (start > len) start = len;
        if (end < start) end = start;
        if (end > len) end = len;

        StringBuilder sb = new StringBuilder();
        sb.append(raw, 0, start);
        for (int i = start; i < end; i++) {
            sb.append(maskChar);
        }
        sb.append(raw.substring(end));
        return sb.toString();
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                log.warn("无法将参数 {} 解析为整数, val={}, 使用默认值 {}", key, val, def);
            }
        }
        return def;
    }
}
