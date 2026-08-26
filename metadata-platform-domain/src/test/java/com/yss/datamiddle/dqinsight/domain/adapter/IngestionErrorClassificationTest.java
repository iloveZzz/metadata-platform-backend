package com.yss.datamiddle.dqinsight.domain.adapter;

import com.yss.datamiddle.dqinsight.client.vo.FieldErrorItem;
import com.yss.datamiddle.dqinsight.domain.constant.DqErrorCodes;
import com.yss.datamiddle.dqinsight.domain.exception.IngestValidationException;
import com.yss.datamiddle.dqinsight.domain.model.ErrorCategory;
import com.yss.datamiddle.dqinsight.domain.util.IngestErrorMessages;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 接入错误分类与脱敏领域测试（WU1：错误分类 format / auth / network → 422 字段级错误）。
 */
class IngestionErrorClassificationTest {

    @Test
    void formatCategoryCarriesErrCodeAndFieldErrors() {
        IngestValidationException exception = new IngestValidationException(
                DqErrorCodes.CSV_SCHEMA, ErrorCategory.FORMAT, "接入解析失败",
                Collections.singletonList(FieldErrorItem.of("row:2.asset_id",
                        DqErrorCodes.CSV_SCHEMA, "asset_id 必填")));

        assertThat(exception.getErrCode()).isEqualTo(DqErrorCodes.CSV_SCHEMA);
        assertThat(exception.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(exception.getFieldErrors()).hasSize(1);
        assertThat(exception.getFieldErrors().get(0).getField()).isEqualTo("row:2.asset_id");
    }

    @Test
    void authCategoryCarriesErrCode() {
        IngestValidationException exception = new IngestValidationException(
                DqErrorCodes.AUTH_INVALID, ErrorCategory.AUTH, "通道认证失败",
                Collections.singletonList(FieldErrorItem.of("Authorization",
                        DqErrorCodes.AUTH_INVALID, "无效的通道 Token")));

        assertThat(exception.getErrCode()).isEqualTo(DqErrorCodes.AUTH_INVALID);
        assertThat(exception.getErrorCategory()).isEqualTo(ErrorCategory.AUTH);
    }

    @Test
    void networkCategoryCarriesErrCode() {
        IngestValidationException exception = new IngestValidationException(
                DqErrorCodes.NETWORK_TIMEOUT, ErrorCategory.NETWORK, "资产校验服务不可用（网络超时）",
                Collections.singletonList(FieldErrorItem.of("assetId",
                        DqErrorCodes.NETWORK_TIMEOUT, "资产校验服务不可用（网络超时）")));

        assertThat(exception.getErrCode()).isEqualTo(DqErrorCodes.NETWORK_TIMEOUT);
        assertThat(exception.getErrorCategory()).isEqualTo(ErrorCategory.NETWORK);
    }

    @Test
    void errorMessageSummaryDoesNotLeakPayloadValues() {
        String summary = IngestErrorMessages.summary(ErrorCategory.FORMAT,
                Arrays.asList(
                        FieldErrorItem.of("row:2.rule_type", DqErrorCodes.CSV_SCHEMA, "rule_type 枚举不合法"),
                        FieldErrorItem.of("row:3.status", DqErrorCodes.CSV_SCHEMA, "status 枚举不合法"),
                        FieldErrorItem.of("row:4.execution_time", DqErrorCodes.CSV_SCHEMA, "时间不合法"),
                        FieldErrorItem.of("row:5.asset_id", DqErrorCodes.CSV_SCHEMA, "asset_id 必填")));

        assertThat(summary).contains("row:2.rule_type", "row:3.status", "row:4.execution_time");
        assertThat(summary).contains("错误分类 format");
        assertThat(summary).doesNotContain("err.dq.auth.invalid");
    }

    @Test
    void exceptionMessageNeverContainsPayloadOrCredentialText() {
        String payloadFragment = "Bearer super-secret-channel-token";
        IngestValidationException exception = new IngestValidationException(
                DqErrorCodes.AUTH_INVALID, ErrorCategory.AUTH, "通道认证失败：无效或缺失的通道 Token",
                Collections.singletonList(FieldErrorItem.of("Authorization",
                        DqErrorCodes.AUTH_INVALID, "无效的通道 Token")));

        assertThat(exception.getMessage()).doesNotContain(payloadFragment);
        assertThat(exception.getFieldErrors().get(0).getMessage()).doesNotContain(payloadFragment);
    }

    @Test
    void csvParseErrorClassificationIsFormat() {
        CsvIngestionAdapter adapter = new CsvIngestionAdapter();
        IngestParseResult result = adapter.parse("asset_id,rule_name\nasset-1,r\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getErrorCode()).isEqualTo(DqErrorCodes.CSV_SCHEMA);
    }

    @Test
    void jsonParseErrorClassificationIsFormatInvalid() {
        ApiJsonIngestionAdapter adapter = new ApiJsonIngestionAdapter();
        IngestParseResult result = adapter.parse("{broken");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCategory()).isEqualTo(ErrorCategory.FORMAT);
        assertThat(result.getErrorCode()).isEqualTo(DqErrorCodes.FORMAT_INVALID);
    }

    @Test
    void parseFailedBatchCarriesSanitizedErrorMessage() {
        CsvIngestionAdapter adapter = new CsvIngestionAdapter();
        IngestParseResult result = adapter.parse("asset_id,field_name,rule_name,rule_type,status,"
                + "failure_reason,execution_time,batch_no\n"
                + "asset-1,,r,format,passed,,2026-08-11T10:00:00+08:00,b-1\n"
                + "asset-2,,r2,bad-type,passed,,2026-08-11T10:00:00+08:00,b-1\n");

        String summary = IngestErrorMessages.summary(result.getErrorCategory(), result.getFieldErrors());

        assertThat(summary).contains("row:3.rule_type");
        assertThat(summary).doesNotContain("bad-type");
    }
}
