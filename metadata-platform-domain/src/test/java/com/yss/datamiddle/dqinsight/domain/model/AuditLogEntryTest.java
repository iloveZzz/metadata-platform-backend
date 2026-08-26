package com.yss.datamiddle.dqinsight.domain.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计记录领域测试（DQI-SLICE-05-WU2，C27 只读不可变 append-only）。
 *
 * <p>AuditLogEntry 为不可变值对象（final 字段 + 无 setter，仅工厂构造）；7 类 action 工厂
 * （ingest / parse-fail / health-calc / channel-config / channel-toggle / channel-retry /
 * linkage-map，SB-08）生成正确 action / result / object / detail；channel-retry 支持
 * success / failure 双结果。审计写入仅 INSERT（网关实现层强制，契约测试覆盖）。</p>
 */
class AuditLogEntryTest {

    @Test
    void entryIsImmutableValueObject() {
        // 所有业务字段 final 且无 setter（只读不可变，append-only 语义的领域层保证）
        Field[] fields = AuditLogEntry.class.getDeclaredFields();
        assertThat(fields).isNotEmpty();
        for (Field field : fields) {
            assertThat(Modifier.isFinal(field.getModifiers()))
                    .as("字段 %s 应为 final（不可变）", field.getName())
                    .isTrue();
        }
        assertThat(Arrays.stream(AuditLogEntry.class.getMethods())
                .noneMatch(m -> m.getName().startsWith("set")))
                .as("AuditLogEntry 不应暴露 setter")
                .isTrue();
    }

    @Test
    void ingestFactoryRecordsActionAndResult() {
        AuditLogEntry entry = AuditLogEntry.ingest("ge-tool", "batch-001", "rows=120");
        assertThat(entry.getAction()).isEqualTo(AuditAction.INGEST);
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(entry.getOperator()).isEqualTo("ge-tool");
        assertThat(entry.getObject()).isEqualTo("batch-001");
        assertThat(entry.getDetail()).contains("rows=120");
        assertThat(entry.getEventTime()).isNotNull();
    }

    @Test
    void parseFailFactoryRecordsFailure() {
        AuditLogEntry entry = AuditLogEntry.parseFail("ge-tool", "batch-002", "CSV schema 违反 row:3");
        assertThat(entry.getAction()).isEqualTo(AuditAction.PARSE_FAIL);
        assertThat(entry.getResult()).isEqualTo(AuditResult.FAILURE);
    }

    @Test
    void healthCalcFactoryRecordsCalculation() {
        AuditLogEntry entry = AuditLogEntry.healthCalc("system", "batch-003", "ruleVersion=v3, assets=10");
        assertThat(entry.getAction()).isEqualTo(AuditAction.HEALTH_CALC);
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(entry.getObject()).isEqualTo("batch-003");
    }

    @Test
    void channelConfigFactoryRecordsConfigurationChange() {
        AuditLogEntry entry = AuditLogEntry.channelConfig("ops-user", "通道A", "schedule 变更");
        assertThat(entry.getAction()).isEqualTo(AuditAction.CHANNEL_CONFIG);
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
    }

    @Test
    void channelToggleFactoryRecordsToggle() {
        AuditLogEntry entry = AuditLogEntry.channelToggle("ops-user", "通道A", "停用");
        assertThat(entry.getAction()).isEqualTo(AuditAction.CHANNEL_TOGGLE);
    }

    @Test
    void channelRetryFactorySupportsBothResults() {
        AuditLogEntry ok = AuditLogEntry.channelRetry("ops-user", "通道A", "拉取成功", AuditResult.SUCCESS);
        AuditLogEntry fail = AuditLogEntry.channelRetry("ops-user", "通道A", "网络超时", AuditResult.FAILURE);
        assertThat(ok.getAction()).isEqualTo(AuditAction.CHANNEL_RETRY);
        assertThat(ok.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(fail.getResult()).isEqualTo(AuditResult.FAILURE);
    }

    @Test
    void linkageMapFactoryRecordsManualMapping() {
        AuditLogEntry entry = AuditLogEntry.linkageMap("ops-user", "batch-004",
                "sourceAssetId=ext-1, resolvedAssetId=asset-9");
        assertThat(entry.getAction()).isEqualTo(AuditAction.LINKAGE_MAP);
        assertThat(entry.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(entry.getDetail()).contains("resolvedAssetId=asset-9");
    }

    @Test
    void allSevenActionsEnumValuesMatchFrozenContract() {
        assertThat(AuditAction.values())
                .extracting(AuditAction::getCode)
                .containsExactly("ingest", "parse-fail", "health-calc", "channel-config",
                        "channel-toggle", "channel-retry", "linkage-map");
    }
}